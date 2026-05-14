# Verify: go

Verification ladder for Go modules. Run from the module root and prefer full-project scope with `./...`.

> L1 is the quick local/CI gate. L2 is the normal pre-review gate. L3 is the full gate when Docker-backed integration or e2e tests exist.

## Commands per step

### L1 — Quick

| Step | Command | Evidence |
|------|---------|----------|
| 1 Build / type-check | `go build ./...` | all packages compile |
| 2 Vet | `go vet ./...` | standard static checks pass |
| 3 Lint | `golangci-lint run ./...` | project lint profile passes |
| 4 Module diff | `go mod tidy -diff` | no unexpected `go.mod` / `go.sum` drift |
| 5 Architecture rules (optional) | `go-arch-lint check` | package boundary rules pass |

Use L1 for fast feedback before claiming compile or lint health.

### L2 — Standard (includes L1)

| Step | Command | Evidence |
|------|---------|----------|
| 6 Unit tests | `go test ./...` | all non-integration tests pass |
| 7 Race detector | `go test -race ./...` | no detected data races |

L2 excludes files guarded by `//go:build integration` unless the project also includes those tags in default test config.

### L3 — Full (includes L2)

| Step | Command | Evidence |
|------|---------|----------|
| 8 Integration tests | `go test -tags=integration ./...` | Docker-backed integration tests pass |
| 9 E2E tests (optional) | `go test -tags=e2e ./...` | end-to-end flows pass |
| 10 Coverage report (optional) | `go test -cover -coverprofile=cover.out ./...` | coverage profile generated |

L3 usually requires Docker when tests use `testcontainers-go` for Postgres, Redis, Kafka, or other services.

## Recommended command groups

Quick gate:

```bash
go build ./...
go vet ./...
golangci-lint run ./...
go mod tidy -diff
```

Standard gate:

```bash
go build ./...
go vet ./...
golangci-lint run ./...
go mod tidy -diff
go test ./...
go test -race ./...
```

Full gate:

```bash
go build ./...
go vet ./...
golangci-lint run ./...
go mod tidy -diff
go test ./...
go test -race ./...
go test -tags=integration ./...
go test -tags=e2e ./...
```

If the project does not define `e2e` tests, skip the final command and say it was not applicable.

## Auto-fix commands

Auto-fix is related to verification but is not itself proof that the project is healthy.

```bash
gofmt -w .
goimports -w .
golangci-lint run --fix ./...
```

Rules:
- Run `gofmt` for all Go edits.
- Run `goimports` when imports changed.
- Re-run the relevant L1/L2 command after auto-fix.
- Do not hide lint findings by disabling linters without an explicit project decision.

## Notes

### Taskfile detection (project automation, check first)

If the project root contains `Taskfile.yaml` (or `Taskfile.yml`), **prefer the project's own task wrappers** over raw `go` commands. The project author has typically pre-tuned race-detector flags, coverage thresholds, lint config, and integration setup (venv creation, container reuse, S3 prep, etc.). Use raw commands only as a fallback.

Detection: run `task --list` (or read `Taskfile.yaml`) to see available tasks.

| GSL level | Preferred tasks (try in order) | Fallback |
|-----------|--------------------------------|----------|
| L1 quick | `task lint` → `task vet` → `task build` | `go vet ./... && go build ./... && golangci-lint run ./...` |
| L2 standard | `task test:short` → `task test` | `go test ./... && go test -race ./...` |
| L3 full | `task verify` → `task test:integration` | `go test -tags=integration ./...` |

Adopt the project's verify ladder as the source of truth for that project — even if its order or scope differs from the raw-command ladder above. Record the chosen ladder in `plan/conventions.md` under `Verification`.

Scope:
- Use `./...` from module root for build, vet, test, and lint unless the project documents a narrower target.
- For workspaces, run verification in each touched module or use the repository's documented workspace command.
- If generated code is involved, run the generator before verification only when the project expects generated files to be committed.

Docker:
- `go test -tags=integration ./...` may start containers through `testcontainers-go`.
- Verify Docker is running before L3; do not treat Docker startup failure as a code test failure without evidence.

Timeouts:
- Quick build/vet/lint: 60-180s depending on module size.
- Unit tests: 180-300s.
- Race tests: 300-600s.
- Integration tests: 600s or project-specific timeout.

Useful flags:

```bash
go test -count=1 ./...              # bypass test cache when checking behavior changes
go test -run TestName ./...         # focused reproduction before full run
go test -timeout=10m ./...          # explicit timeout for CI or long packages
go test -json ./...                 # machine-readable output for CI parsers
```

Coverage:

```bash
go test -cover -coverprofile=cover.out ./...
go tool cover -func=cover.out
go tool cover -html=cover.out
```

Coverage is supporting evidence. It does not replace behavior-focused assertions.

## Git hook examples

Pre-commit quick hook:

```bash
gofmt -w . && goimports -w . && go test ./...
```

Pre-push standard hook:

```bash
go build ./... && go vet ./... && golangci-lint run ./... && go test -race ./...
```

## Completion report template

When reporting completion, include exact commands and outcomes:

```text
검증:
- go build ./...: 통과
- go test ./...: 통과
- go test -tags=integration ./...: 미실행, Docker 필요
```

Never claim completion from intent alone. Run the command, read the output, then report the evidence.

