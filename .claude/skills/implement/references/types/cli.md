# Type: cli
Language-independent patterns for command-line applications. Pair with `languages/{language}.md` for concrete code.
## Layer Breakdown (universal)
```
argv / env / stdin
  -> [Command binding] cobra / click / typer / argparse / yargs
  -> [Handler / use case] validate command intent and map args to plain input
  -> [Service] orchestrate dependencies and application behavior
  -> [Domain] pure rules and reusable operations
```
Rules:
- Command binding is thin: names, args, flags, help, examples, dispatch.
- Handler owns CLI semantics: input mapping, writer choice, exit-code mapping.
- Service owns behavior: no terminal color, process exit, or parser dependency.
- Domain owns rules: testable without a shell or framework.
- Exit once at the process edge; return codes through the command path.
## Command Binding
| Ecosystem | Binding examples | Typical location |
|-----------|------------------|------------------|
| Go | cobra | `cmd/{tool}` or `internal/cli` |
| Python | click / typer / argparse | `src/{package}/cli.py` |
| Node | yargs / commander | `bin/{tool}` and `src/cli` |
| Rust | clap | `src/cli.rs` or `src/main.rs` |
| Java/Kotlin | picocli | command class delegating to service |
Binding responsibilities:
- define command and subcommand names
- declare positional arguments and flags
- provide help text, defaults, and examples
- apply parser-level required checks
- call one handler with a plain input object
Keep out of binding:
- business branching
- persistence and network workflows
- output formatting beyond writer selection
- `exit`, `panic`, or uncaught exception paths
## Argument Design
### Positional arguments
Use positional arguments for required identity-like values:
```
tool user get <user-id>
tool file convert <input> <output>
```
Good candidates:
- one resource id
- one input path
- one required target
- values that read naturally in command form
Avoid positional arguments when:
- there are more than two or three values
- two values have the same type and can be swapped
- the value is optional
- order is hard to remember
### Flags
Use flags for options, filters, formats, and modes:
```
tool users list --status active --limit 50 --json
tool import data.csv --dry-run --batch-size 500
```
Flag rules:
- Prefer stable long flags for scripts.
- Add short flags only for common interactive actions such as `-v`, `-q`, `-h`.
- Boolean flags should be positive by default, with `--no-*` where supported.
- Defaults must appear in help text or docs.
- Do not make a flag meaning depend on hidden state.
### Subcommands
Group by resource and action:
```
tool project create
tool project list
tool project delete <id>
tool config get <key>
tool config set <key> <value>
```
Common grouping patterns:
- resource first: `user create`, `user list`, `user delete`
- workflow first: `deploy start`, `deploy status`, `deploy rollback`
- admin namespace: `admin repair`, `admin backfill`, `admin inspect`
Framework notes:
- cobra: nested `*cobra.Command`
- click: `@click.group()` plus subcommands
- typer: mounted `Typer()` apps
- argparse: `add_subparsers()`
- yargs: `.command()` modules or chains
## Output Conventions
### stdout / stderr / exit code
| Channel | Purpose |
|---------|---------|
| stdout | command result intended for caller or pipeline |
| stderr | logs, prompts, progress, warnings, diagnostics |
| exit code | machine-readable success or failure status |
Rules:
- stdout must stay parseable in pipelines.
- progress bars and spinners go to stderr.
- warnings go to stderr even on success.
- error details go to stderr unless JSON error output is explicitly requested.
- never mix logs into JSON stdout.
### Human-readable output
Default output should work well in a terminal:
```
ID        STATUS    NAME
p_123     active    search-index
p_124     paused    newsletter
```
Guidelines:
- Use tables for short list output.
- Use key/value blocks for detail output.
- Keep default output readable, but do not treat it as a stable API.
- Avoid decorative output that breaks narrow terminals.
### Machine-readable output
Provide JSON for automation:
```
tool project list --json
```
JSON rules:
- stdout contains only JSON.
- stderr may contain warnings unless `--quiet` is set.
- Use stable field names.
- Prefer arrays for lists and objects for details.
- Do not emit trailing summary text after JSON.
### Quiet / verbose / color
| Mode | stdout | stderr |
|------|--------|--------|
| default | result | warnings and useful progress |
| `--quiet` | result only | errors only |
| `--verbose` | result | detailed steps |
| `--json` | JSON result | diagnostics only |
Color rules:
- Enable color only when the relevant stream is a TTY.
- Honor `--color`, `--no-color`, and `NO_COLOR` where practical.
- Use a terminal library or output abstraction.
- Do not scatter raw ANSI escape strings through handlers.
- Never rely on color as the only meaning.
## Exit Codes
| Code | Meaning | Use |
|------|---------|-----|
| 0 | success | command completed successfully |
| 1 | user error | invalid args, validation failure, not found, conflict |
| 2 | system error | network, database, filesystem, or unexpected internal failure |
| 130 | interrupted | SIGINT / Ctrl-C, POSIX convention |
Guidelines:
- Keep numeric codes small and documented.
- Map parser and validation failures to user error.
- Map dependency failures to system error.
- Put stable domain error codes in text or JSON, not only in exit code.
## Configuration Precedence
Use this order, highest priority first:
```
flags > environment variables > config file > defaults
```
Rules:
- Flags override every other source.
- Environment variables are useful for CI and containers.
- Config files are useful for project or user defaults.
- Defaults must be explicit.
- Verbose or debug mode may print effective config with secrets redacted.
- Never print tokens, passwords, private keys, or signed URLs.
## Long-running Operations
Progress reporting:
- write progress to stderr
- use progress bars only for TTY sessions
- use periodic line logs for non-TTY sessions
- include counts, current phase, and elapsed time when useful
- keep stdout reserved for results
Partial output and signals:
- document whether stdout may be partial after interruption
- flush at safe record boundaries
- prefer newline-delimited JSON for streams
- handle SIGINT and SIGTERM through cancellation
- stop accepting new work, flush buffers, persist checkpoint when supported
- return 130 for SIGINT
## Testability
Design handlers as plain functions:
```
handle(input, deps, stdout, stderr) -> exit_code
```
Test seams:
- args can be constructed in memory
- stdout and stderr writers are injectable
- environment lookup is injectable
- filesystem and network access sit behind small interfaces
- clock and random id generation are injectable when behavior depends on them
Recommended tests:
- parser test: argv maps to input object
- handler test: input maps to service call and output
- service/domain test: behavior without CLI framework
- smoke test: real process exits with expected code for core paths
## Folder Layout per Phase
```
cmd/{tool}/main.*          # process entrypoint
src/cli.*                 # command binding
src/handlers/*            # command handlers
src/services/*            # use cases
src/domain/*              # pure rules
```
Language variants:
- Go: `cmd/{tool}/main.go`, `internal/cli`, `internal/{domain}`
- Python: `src/{package}/__main__.py`, `cli.py`, `handlers/`, `services/`
- Node: `bin/{tool}.js`, `src/cli/commands`, `src/services`, `src/domain`
Phase rule:
- Phase 1: service/domain behavior and handler contract.
- Phase 2: CLI binding, help text, argument validation.
- Phase 3: packaging, shell completion, install docs, smoke tests.
## Common Anti-patterns
- One function contains parsing, business logic, and printing.
- Service code calls `exit` or terminates the process.
- User errors are handled with `panic` or uncaught exceptions.
- Hard-coded absolute paths.
- Hidden config precedence.
- Logs or progress are printed to stdout.
- JSON stdout includes human summary text.
- Raw ANSI escape strings are written directly from handlers.
- Color is enabled without TTY detection.
- Automation commands prompt without non-interactive mode.
- Retry loops have no timeout or cancellation.
- Dependency failures are swallowed and reported as success.
