# Verify: java-gradle

Production verification ladder for multi-module Gradle projects (Java + optional Kotlin sub-module). Generalized from `bottle-note-api-server`.

> Pair with `languages/java-spring.md` (patterns) and `testing/java.md` (test infrastructure).

## Tag-filtered task setup (build.gradle)

The patterns below assume tag-filtered Gradle tasks. If not present in the project, add them once in the root `build.gradle`:

```groovy
allprojects {
    tasks.register('unit_test', Test) {
        useJUnitPlatform { includeTags 'unit' }
        // separate result dir to avoid clobbering default `test`
    }
    tasks.register('integration_test', Test) {
        useJUnitPlatform { includeTags 'integration' }
    }
    tasks.register('check_rule_test', Test) {
        useJUnitPlatform { includeTags 'rule' }
    }
    tasks.register('admin_integration_test', Test) {
        useJUnitPlatform { includeTags 'admin_integration' }
    }
}
```

## Commands per step

### L1 — Quick (~2 min)

| Step | Command | What it catches |
|------|---------|-----------------|
| 1 Compile Java | `./gradlew compileJava compileTestJava` | Type errors, missing imports across all modules |
| 2 Compile Kotlin (if present) | `./gradlew :{kotlin-module}:compileKotlin :{kotlin-module}:compileTestKotlin` | Kotlin sub-module type errors |
| 3 Architecture rules | `./gradlew check_rule_test` | ArchUnit rule violations (package boundaries, layer crossings, annotation usage) |

### L2 — Standard (~5 min, includes L1)

| Step | Command | What it catches |
|------|---------|-----------------|
| 4 Unit tests | `./gradlew unit_test` | Business logic regressions (tag-filtered `@Tag("unit")`) |
| 5 Full build | `./gradlew build -x test -x asciidoctor --build-cache --parallel` | JAR packaging, classpath resolution, dependency graph |

`--build-cache --parallel` cuts L2 from ~8 min to ~5 min on warm cache. Use `-x test -x asciidoctor` to avoid duplicate test runs (already done in step 4) and slow doc generation.

### L3 — Full (~15 min, includes L2)

| Step | Command | What it catches |
|------|---------|-----------------|
| 6 Integration tests | `./gradlew integration_test` (requires Docker) | DB schema drift, real-IO failures (TestContainers spins up MySQL, Redis, MinIO) |
| 7 Admin integration (if present) | `./gradlew admin_integration_test` (requires Docker) | Admin-surface integration tests with Kotlin/Java interop, context-path |
| 8 (optional) Doc build | `./gradlew asciidoctor` | RestDocs / AsciiDoc generation, snippet references |

L3 = final gate before `git push` / PR. During active development, L1/L2 is sufficient.

## Auto-fix commands (run before each commit)

- Format (Spotless / google-java-format): `./gradlew spotlessApply`
- Apply ArchUnit fix suggestions: (manual — ArchUnit only reports, does not auto-fix)

## CI parity

A typical CI job (e.g., GitHub Actions) maps onto L1+L2+L3:

```yaml
- run: ./gradlew compileJava compileTestJava :{kotlin-module}:compileKotlin
- run: ./gradlew check_rule_test
- run: ./gradlew unit_test
- run: ./gradlew build -x test -x asciidoctor --build-cache --parallel
- run: ./gradlew integration_test admin_integration_test    # requires Docker service
```

Running `/verify full` locally mirrors CI — divergence is a project smell.

## Coverage (optional)

If JaCoCo is configured:

```bash
./gradlew unit_test jacocoTestReport
./gradlew jacocoTestCoverageVerification    # fails build below threshold
```

Combine reports across modules with `jacocoMergedReport` task (project-specific). Coverage targets typically live in `build.gradle` per module.

## Bash timeouts (when invoking via tooling)

Recommended timeouts for shell automations / Claude Code Bash tool:

| Step | Timeout |
|------|---------|
| Compile (Java / Kotlin) | 120 s |
| Format / rule tests | 180 s |
| Unit tests | 300 s |
| Full build | 300 s |
| Integration tests | 600 s |
| Admin integration | 600 s |
| Doc build (asciidoctor) | 300 s |

Containers are reused (`withReuse(true)`), so steady-state integration is faster than first run. Allow first-run cold-start headroom.

## Baseline check (run BEFORE the first L1 on a new feature branch)

Before running any verification level on your changes, confirm the current branch base (typically `main`) is green:

```bash
./gradlew check_rule_test
```

If the baseline is already **RED** (existing ArchUnit / lint rule violations on `main` unrelated to your work), isolate them from your feature work:

- **Option A**: Fix the baseline first as a separate commit / PR, then resume feature work on a clean tree
- **Option B**: Record the known-red rules in your `plan/{feature}.md` so `/debug` does NOT chase pre-existing failures during your feature's `/verify` runs

This prevents the most common waste of `/debug` time — investigating failures that existed before your change.

Skip the baseline check only if you have verified within the last hour that the branch base is green.

## Notes

- **Full project scope always** — modules typically share a `core`/`mono` library. Per-module check misses cross-module breakage. `./gradlew compileJava` from the root walks all subprojects.
- **L3 requires Docker** for TestContainers (MySQL, Redis, MinIO, etc.). Without Docker → fall back to L2 and report explicitly; do NOT silently pass.
- **Stop on first failure** — `/verify` exits at step N and reports last 30 output lines. Do not continue to step N+1.
- **Submodule init** — if the project uses git submodules for environment scripts (`storage/mysql/init/*.sql`), run `git submodule update --init --recursive` once before L3. Otherwise integration containers may start without seed data.
- **Spotless drift** — `spotlessCheck` (part of `build`) fails on format drift; auto-fix with `./gradlew spotlessApply`. Treat as compile-level: format drift = L1 fail.
- **Daemon recycle** — long-running Gradle daemons can leak memory across runs. If verification times balloon, `./gradlew --stop` and retry.

## Anti-patterns

- Running `./gradlew test` (the default unscoped task) — runs ALL tests including slow integration, defeating tag-filtering
- Per-module verification (`./gradlew :module-x:test`) as final evidence — misses cross-module impact
- Skipping L3 before push because "tests passed locally" — L3 is the only level that catches real-IO regressions
- Continuing past a step failure to "see what else breaks" — the failure cascade is noise; fix step N first
- Reporting L2 PASS as L3 PASS when Docker is unavailable — must be explicit about which level actually ran
