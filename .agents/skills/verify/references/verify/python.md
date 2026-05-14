# Verify: python

Verification ladder for Python 3.12+ projects. Run the smallest level that proves the requested change, then report exact commands and outcomes.

> Use with `languages/python.md` and `testing/python.md`.

## L1 — Quick Static Verification

Use L1 for formatting, linting, typing, and architecture import rules.

| Step | Command |
|------|---------|
| 1 Type-check | `mypy src/` |
| 1 Alternative type-check | `pyright` |
| 2 Lint | `ruff check .` |
| 3 Format check | `ruff format --check .` |
| 4 Import / architecture rules | `import-linter --config .importlinter` |

Run `mypy` or `pyright` according to the project standard. Do not run both unless the project already does.

Recommended combined command:

```bash
mypy src/ && ruff check . && ruff format --check .
```

With import-linter:

```bash
mypy src/ && ruff check . && ruff format --check . && import-linter --config .importlinter
```

## L2 — Standard Verification

L2 includes L1 plus unit tests and package build.

| Step | Command |
|------|---------|
| 1 Static verification | L1 commands |
| 2 Unit tests | `pytest -m "unit and not integration" --strict-markers` |
| 3 Package build | `python -m build` |
| 3 Alternative build | `uv build` |

Recommended combined command:

```bash
mypy src/ \
  && ruff check . \
  && ruff format --check . \
  && pytest -m "unit and not integration" --strict-markers \
  && python -m build
```

Use `uv build` instead of `python -m build` when the project standardizes on uv.

## L3 — Full Verification

L3 includes L2 plus integration tests.

| Step | Command |
|------|---------|
| 1 Standard verification | L2 commands |
| 2 Integration tests | `pytest -m integration --strict-markers` |
| 3 Optional E2E / smoke | `pytest -m e2e --strict-markers` |

Recommended command:

```bash
mypy src/ \
  && ruff check . \
  && ruff format --check . \
  && pytest -m "unit and not integration" --strict-markers \
  && python -m build \
  && pytest -m integration --strict-markers
```

Integration tests that use `testcontainers-python` require Docker.

## Coverage Commands

Use coverage when the change touches business logic or CI requires a gate.

Unit coverage:

```bash
pytest -m "unit and not integration" \
  --strict-markers \
  --cov=app \
  --cov-report=term-missing
```

CI gate:

```bash
pytest \
  --strict-markers \
  --cov=app \
  --cov-report=xml \
  --cov-report=term-missing \
  --cov-fail-under=80
```

`--cov-fail-under=N` fails the command when total coverage is below `N`.

## Auto-fix Commands

Auto-fix commands are not verification. Run verification again after auto-fix.

Format:

```bash
ruff format .
```

Lint fix:

```bash
ruff check --fix .
```

Combined:

```bash
ruff format . && ruff check --fix .
```

Use `ruff check --fix --unsafe-fixes .` only after reviewing what unsafe fixes may change.

## pytest Options

Recommended defaults in `pyproject.toml`:

```toml
[tool.pytest.ini_options]
addopts = [
  "--strict-markers",
  "--strict-config",
]
markers = [
  "unit: fast tests without external infrastructure",
  "integration: tests requiring Docker or real infrastructure",
  "docs: documentation and generated contract tests",
  "e2e: full-stack end-to-end tests",
]
```

`--strict-markers` catches marker typos such as `@pytest.mark.integraton`.

## pre-commit Pattern

Use pre-commit for fast local checks:

```yaml
repos:
  - repo: https://github.com/astral-sh/ruff-pre-commit
    rev: v0.5.0
    hooks:
      - id: ruff
        args: [--fix]
      - id: ruff-format
```

Keep type-check and tests in CI even if pre-commit exists.

## Notes

- Run verification at whole-project scope: `src/`, `tests/`, or `.` as configured.
- Avoid per-file verification as final evidence unless the project is extremely large and the user requested a narrow check.
- L3 requires Docker when integration tests use testcontainers.
- Recommended timeouts: L1 60s, L2 180s, L3 600s.
- If Docker is unavailable, report L1/L2 results separately from skipped L3.
- If `python -m build` fails because `build` is missing, install through the project toolchain, for example `uv add --dev build` or project-approved equivalent.
- Do not claim completion from auto-fix output alone. Re-run the relevant verification level.

