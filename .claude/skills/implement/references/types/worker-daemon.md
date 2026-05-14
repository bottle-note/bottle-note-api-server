# Type: worker-daemon

Language-independent patterns for long-running worker processes that consume tasks from a queue / message broker and execute them indefinitely. Distinct from `cli` (one-shot command) and `batch` (scheduled job with defined start/end).

> Use with the matching `languages/{language}.md`. Common languages: Go (NATS / pgmq / Redis Streams), Java (Spring Cloud Stream / Kafka), Python (Celery / Dramatiq).

## When to choose `worker-daemon` over neighbors

| Symptom | Type |
|---------|------|
| Reads from queue indefinitely, no scheduled stop | **worker-daemon** ✓ |
| Triggered by cron / specific time, finite work | batch |
| Single user-facing command invocation, finite | cli |
| Receives HTTP requests, finite per-request | web-api |

Common hybrid: a process launched by **CLI** (`./mytool worker --queue=tasks`) but **operates as a worker-daemon**. Reference selection: primary = `worker-daemon.md`, secondary = `cli.md` for the launch surface.

## Layer Breakdown

```
[Launch surface]   CLI binding / main() / k8s entrypoint    — boot, parse env/flags
       ↓
[Bootstrap]        config load, logger, metrics, signal trap
       ↓
[Broker consumer]  NATS / Kafka / Redis Streams / pgmq client
       ↓
[Worker registry]  task type → handler function map
       ↓
[Task handler]     business logic per task type
       ↓
[Service / repo]   shared domain layer (same as web-api / batch)
```

## Worker Registry Pattern

Tasks have a `type` field; registry dispatches to the correct handler.

```
type WorkerHandler func(ctx context.Context, payload []byte, out OutputPort) error

type Registry struct {
    handlers map[string]WorkerHandler
}

func (r *Registry) Register(taskType string, h WorkerHandler) { ... }
func (r *Registry) Dispatch(ctx, msg) error {
    h, ok := r.handlers[msg.Type]
    if !ok { return ErrUnknownTaskType }   // → ack with DLQ tag
    return h(ctx, msg.Payload, msg.Out)
}
```

Rule: a new worker type = a new entry in `Register(...)` + a new handler file. Never modify existing handlers when adding a type.

## Port Schema (input / output contract)

Each handler declares its input and output schemas (JSON Schema / Pydantic / Go struct + tag):

```
worker: pdf-extract
input:  { uri: string, language?: string }
output: { text: string, page_count: int }
errors: { code: "INVALID_URI" | "EXTRACT_FAILED" | "DOWNLOAD_TIMEOUT" }
```

Schemas live in `pkg/schema/definitions/` (language-specific path). Generator produces typed models. Handlers operate on typed inputs, not raw bytes.

## Broker Integration

| Broker | Acknowledgment | Retry | DLQ |
|--------|---------------|-------|-----|
| NATS JetStream | `msg.Ack()` after success | Stream config: `MaxDeliver` | `MaxDeliver` exceeded → DLQ subject |
| Kafka | Commit offset on success | Manual loop with backoff | `__dlq` topic |
| Redis Streams | `XACK` on success | `XCLAIM` for pending > timeout | Separate stream |
| pgmq (Postgres) | `archive(msg_id)` | `read_with_poll` re-delivers if not archived | Separate queue table |

Rule: ack ONLY after the handler returns success. On failure: do NOT ack — let the broker re-deliver. Handler must be idempotent (same task delivered twice → same result).

## Graceful Shutdown

```
Signal received (SIGTERM / SIGINT)
    ↓
1. Stop accepting NEW tasks (consumer.Unsubscribe / cancel context)
2. Drain in-flight: wait for current handlers to finish (with timeout, e.g., 30s)
3. Flush metrics, close DB connections, close broker connection
4. Exit 0
```

Hard rule: NEVER `os.Exit(0)` while a task is in-flight. The broker thinks the task succeeded but no ack was sent → re-delivery + possible double-execution.

## Idempotency

Tasks may be delivered more than once (at-least-once semantics in most brokers). Handlers MUST be idempotent:

- **Dedup key**: include `task_id` (UUID) in every task. Handler checks a fast store (Redis `SETNX` with TTL, or DB `UNIQUE` index) before doing work. If already processed → ack and skip.
- **Idempotent writes**: prefer `INSERT ... ON CONFLICT DO NOTHING` / `MERGE` over plain `INSERT`. External API calls should accept `Idempotency-Key` header.
- **State machine**: if the task transitions a record from state A → B, check current state first. Re-running on a B record should no-op.

## Observability (mandatory in worker-daemon)

- **Structured logs**: every log line carries `task_id`, `task_type`, `worker_id`, `attempt`. JSON output, NOT plain text.
- **Metrics**:
  - `worker_tasks_processed_total{task_type, outcome=success|failure|dlq}` (counter)
  - `worker_task_duration_seconds{task_type}` (histogram)
  - `worker_in_flight{task_type}` (gauge)
  - `worker_queue_lag_seconds` (gauge, from broker)
- **Tracing**: propagate `trace_id` from message header. Span = handler invocation.
- **Health endpoints** (HTTP, separate port from broker):
  - `/healthz` — process alive
  - `/readyz` — broker connection healthy, handler registry initialized
- **Run history**: write to a `worker_runs` table (`task_id`, `started_at`, `finished_at`, `outcome`, `error_summary`)

## Resource Limits

- **Concurrent task limit**: semaphore / channel-buffered worker pool. Default = number of CPU cores. Tune per task type if some are I/O-heavy.
- **Per-task timeout**: `context.WithTimeout` at handler entry. Reasonable default 5 min; configurable per task type.
- **Memory limit**: rely on container limits (k8s `resources.limits.memory`). Process should fail fast (OOM) rather than swap.
- **Backpressure**: when broker re-delivery rate exceeds processing rate, log + emit alert. Optionally pause subscription.

## Folder Layout per Phase

```
src/{root}/
├── cmd/                       # CLI binding (Phase 2 surface)
│   └── worker.go              # parses flags, calls bootstrap
├── internal/{root}/ (or pkg/)
│   ├── bootstrap/             # config, logger, metrics, signals
│   ├── broker/                # broker-specific client (NATS, Kafka, ...)
│   ├── workers/               # one file per worker type
│   │   ├── pdf_extract.go
│   │   ├── thumbnail_resize.go
│   │   └── registry.go        # GetBuiltinWorkers() / Register
│   ├── schema/                # input/output JSON schemas
│   └── observability/         # metrics, tracing, health endpoint
```

## Common Anti-patterns

- **`os.Exit` mid-task**: shutdown signal handler that exits immediately, in-flight task lost
- **No idempotency**: handler does an unconditional `INSERT` — duplicate row on re-delivery
- **Ack-before-success**: handler acks the message, then crashes — task lost
- **Unbounded concurrency**: no worker pool / semaphore — broker re-delivery overwhelms the process
- **Plain-text logs**: `log.Printf("processed task %s", id)` — non-greppable, no structured filters
- **No DLQ wiring**: failed tasks loop forever or get silently dropped
- **Health endpoint inside broker port**: `/healthz` on the same port as worker traffic — k8s probe fails when broker port is busy
- **CLI command mixed with worker logic**: `cmd/worker.go` contains business logic instead of delegating to handler
- **Schema in code only**: input shape defined only in Go struct, not in a generator-friendly schema — frontend / other services can't validate
- **No per-task timeout**: a stuck task holds a worker slot forever, pool starvation
