# Type: batch
Language-independent patterns for batch jobs, scheduled jobs, backfills, and offline workers. Pair with `languages/{language}.md` for concrete code.
## Layer Breakdown (universal)
```
time / event / manual trigger
  -> [Scheduler binding] cron / queue consumer / workflow runner / admin command
  -> [Job] run identity, parameters, lifecycle, observability
  -> [Step] bounded work unit with retry and checkpoint semantics
  -> [Service] orchestration and dependency calls
  -> [Domain] pure rules, transformations, validation
```
Rules:
- Scheduler binding is thin: starts a job with parameters and trigger metadata.
- Job owns lifecycle: run id, status, top-level metrics, and final outcome.
- Step owns progress: pagination, checkpoint, retry boundary, partial failures.
- Service owns behavior: domain operations and external writes.
- Domain owns rules: deterministic logic independent of scheduler runtime.
## Trigger Modes
| Mode | Source | Typical use |
|------|--------|-------------|
| scheduled | cron, platform timer, workflow scheduler | nightly sync, cleanup, aggregation |
| event-driven | queue, topic, object event, webhook | async processing, fan-out work |
| manual | CLI, admin API, runbook command | backfill, repair, one-off migration |
Guidelines:
- Reuse the same job implementation across trigger modes when behavior is the same.
- Store trigger type and parameters in run history.
- Manual runs require explicit scope: date range, ids, shard, or dry-run.
- Event-driven jobs record source event id for deduplication.
## Scheduler Binding
Binding responsibilities:
- parse schedule or event payload
- create or receive a run id
- acquire a job-level lock when overlap is forbidden
- pass plain parameters to the job
- report start and finish status to the scheduler
Keep out of binding:
- record-by-record processing
- database pagination
- retry loops for business operations
- large transformations
- alert suppression logic
## Job and Step Model
A job should expose explicit metadata:
```
job_name: daily-index-refresh
run_id: 2026-05-14T00:00:00Z/01
trigger_type: scheduled
parameters: { date: "2026-05-13" }
status: running | succeeded | failed | canceled
```
Run identity rules:
- Every run has a unique `run_id`.
- Every scheduled occurrence has a stable schedule key.
- Logs and metrics include `job_name` and `run_id`.
- Attempts are tracked with attempt number or child run ids.
Use steps for separate operational boundaries:
```
load candidates -> transform -> write output -> publish summary
```
Step responsibilities:
- bounded input scope
- checkpoint location
- retry policy
- metrics and logs
- error classification
- cleanup of temporary resources
## Idempotency
Batch jobs should assume duplicate execution:
- process restart
- scheduler retry
- manual rerun
- duplicate event delivery
- partial failure after write
### Dedup key
Use a stable key:
```
{job_name}:{business_date}:{source_id}:{operation}
```
Good keys:
- source event id
- source record id plus version
- destination natural key
- job name plus partition plus logical date
Avoid keys based only on processing time.
### Exactly-once vs at-least-once
| Model | Meaning | Practical requirement |
|-------|---------|-----------------------|
| exactly-once | each logical item affects output once | transactional sink or dedup table |
| at-least-once | item may be delivered more than once | idempotent writes |
| at-most-once | item may be lost | only for non-critical telemetry |
Default pattern:
- accept at-least-once execution
- make writes idempotent
- record dedup keys
- make rerun safe
### Idempotent writes
Patterns:
- upsert by natural key
- insert with unique constraint on dedup key
- compare-and-set by version
- write to staging, then atomic promote
- object-store write to deterministic path
- external API call with idempotency key when available
## Checkpointing
Persist progress durably:
- database table
- local file for single-node tools
- object storage such as S3
- workflow engine state
- queue offset managed by consumer group
Checkpoint contents:
```
run_id
step_name
partition_key
last_processed_cursor
records_processed
records_failed
updated_at
```
Rules:
- Save checkpoint after a safe commit boundary.
- Do not checkpoint before the matching output write succeeds.
- Store enough input scope to resume deterministically.
- Version checkpoint format when it may change.
- Validate ownership before resuming an abandoned run.
Resume behavior:
- load latest successful checkpoint
- skip completed partitions
- continue from cursor
- optionally reprocess the last small window
- rely on idempotent writes for overlap
## Partitioning
Partition when one run cannot finish within time or resource limits.
Strategies:
- key range: `id 1..100000`
- hash bucket: `hash(user_id) % N`
- time window: hourly or daily ranges
- tenant: one partition per tenant
- source shard: one partition per upstream shard
Guidelines:
- Partitions should be independent where possible.
- Partition key must appear in logs and metrics.
- Use stable partition assignment for retry.
- Avoid tiny partitions that create scheduler overhead.
- Avoid huge partitions that cannot complete before timeout.
Parallel processing:
- use bounded worker pools
- limit concurrency per dependency
- preserve ordering only when required
- combine partition results after required partitions finish
- treat large partitions as retry and checkpoint boundaries
## Failure Modes
### Error classification
| Type | Example | Action |
|------|---------|--------|
| validation | malformed source record | skip or dead-letter with reason |
| transient | timeout, 503, rate limit | retry with backoff |
| permanent | invalid state, unsupported source | mark failed item or dead-letter |
| system | database down, disk full | fail step or job |
| programmer | invariant violation | fail fast and alert |
### Retry policy
Use bounded exponential backoff:
```
initial_delay = 1s
multiplier = 2
max_delay = 1m
max_attempts = 5
jitter = true
```
Rules:
- Retry only classified transient errors.
- Put max attempts on every retry loop.
- Use jitter for distributed jobs.
- Respect cancellation.
- Record attempt count and final reason.
### Dead-letter handling
Dead-letter records should include:
- source id
- payload reference or redacted payload
- failure reason
- attempt count
- job name, run id, step id
- partition key
- timestamp
Dead-letter is not completion by itself. Define review, replay, or discard policy.
### Alerting threshold
Alert on:
- job failure
- missed run beyond SLA
- error rate above threshold
- dead-letter count above threshold
- run duration above expected window
- repeated retries against the same dependency
## Resource Management
Batch size rules:
- start conservative
- tune with measured duration and memory
- keep transactions short enough for normal lock behavior
- document default batch size
- use smaller batches for large payloads
Connection rules:
- bound database and HTTP client pools
- do not create a new client per record
- set connect, read, and total timeouts
- close idle resources on shutdown
- match worker concurrency to pool size
OOM prevention:
- stream input instead of loading all records
- page through source data
- flush output incrementally
- bound producer and worker queues
- avoid unbounded in-memory dedup maps
## Observability
Structured log fields:
```
job_name
run_id
step_id
batch_id
partition_key
attempt
records_processed
records_failed
duration_ms
error_code
```
Logging rules:
- one start and finish log per job
- one start and finish log per step
- periodic progress logs for long steps
- sample item ids only when safe
- same field names across jobs
Core metrics:
- `records_processed`
- `records_succeeded`
- `records_failed`
- `records_skipped`
- `duration_ms`
- `error_rate`
- `retry_count`
- `dead_letter_count`
- `lag_seconds`
Run history should answer what ran, what scope it used, what changed, when it finished, why it failed, and which code version ran.
## Scheduling
Document every schedule:
- cron expression
- timezone
- expected duration
- owner
- missed-run policy
- overlap policy
Example:
```
0 3 * * * Asia/Seoul daily-report skip-if-running
```
Time rules:
- store timestamps in UTC
- define business date timezone explicitly
- do not infer timezone from server local time
- define daylight saving behavior when relevant
Missed-run policies:
- catch-up: run every missed occurrence
- skip: run only next occurrence
- coalesce: combine missed range into one run
Overlap policies:
- forbid overlap with job lock
- allow overlap by partition
- allow overlap only for different parameters
- cancel older run when newer run starts
## Folder Layout per Phase
```
src/{root}/jobs/{job_name}/
├── scheduler.*      # cron/event/manual binding              (Phase 2)
├── job.*            # lifecycle and orchestration            (Phase 1)
├── steps/           # checkpointed work units                (Phase 1)
├── services/        # domain-facing operations               (Phase 1)
├── domain/          # pure rules and transformations         (Phase 1)
├── repository/      # run history/checkpoint persistence     (Phase 1)
└── observability.*  # metrics/log field helpers              (Phase 2)
```
Phase rule:
- Phase 1: job, steps, idempotent service behavior, checkpoint model.
- Phase 2: scheduler binding, locks, metrics, run history.
- Phase 3: runbook, alerts, dashboard, replay tooling.
## Common Anti-patterns
- Global counters updated by parallel workers without synchronization.
- Long-running processing without durable checkpoint.
- Assuming cron prevents duplicate execution.
- Infinite retry loops without max attempts.
- Retrying permanent validation failures.
- Loading all records into memory before processing.
- Creating database or HTTP clients per item.
- Writing output before dedup or idempotency is defined.
- Logs omit run id, step id, or partition key.
- Scheduler code contains business logic.
- Manual backfill has no explicit scope or dry-run.
- Silent partial failure is reported as success.
