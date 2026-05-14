# Type: library
Language-independent patterns for libraries and reusable packages. Pair with `languages/{language}.md` for concrete code.
## Purpose
A library is consumed by other code. Its main product is a stable public API, not a process, page, or job.
Design goals:
- clear public surface
- small dependency footprint
- predictable versioning
- runnable examples
- tests from the consumer boundary
- documentation for every exported symbol
## Layer Breakdown (universal)
```
consumer application
  -> [Public API] exported functions, classes, types, modules
  -> [Facade] stable entrypoints and configuration objects
  -> [Internal core] implementation, algorithms, adapters
  -> [Dependencies] optional integrations and platform-specific code
```
Rules:
- Public API is intentional: exporting a symbol creates compatibility responsibility.
- Facade is stable: most consumers should not need internal modules.
- Internal core can change: keep it hidden behind public entrypoints.
- Dependencies are conservative: every transitive dependency becomes a consumer concern.
## Public API Surface
Define what is exported and what is internal-only.
Language patterns:
- Java: public classes are API; package-private classes stay internal.
- Kotlin: `public` is default; use `internal` deliberately.
- Python: define `__all__`, prefix internals with `_`, document supported imports.
- Go: exported identifiers start with capital letters; use `internal/` for hard boundaries.
- Rust: `pub` and module visibility define API; avoid broad `pub use` without intent.
- TypeScript: exported symbols from package entrypoints are API.
Rules:
- Keep one or a few documented entrypoints.
- Do not require consumers to import deep internal paths.
- Treat config keys, error codes, and event names as public API if consumers use them.
- Mark experimental APIs with stability level.
- Prefer a small facade over exposing many implementation classes.
API checklist:
- symbol name is stable
- parameter names and types are stable
- return shape is stable
- error behavior is documented
- default behavior is documented
- concurrency expectation is documented when relevant
## Backwards Compatibility
Use SemVer:
```
MAJOR.MINOR.PATCH
```
| Change | Version bump | Example |
|--------|--------------|---------|
| compatible bug fix | PATCH | fix parsing for valid existing input |
| compatible new feature | MINOR | add optional parameter or new function |
| compatible deprecation notice | MINOR | warn that an API will be removed later |
| breaking API removal | MAJOR | remove exported class or function |
| breaking behavior change | MAJOR | same input now means different operation |
| breaking type change | MAJOR | return string becomes object |
Compatible changes:
- add a new exported symbol
- add an optional config key with default
- improve performance without changing behavior
- fix behavior that was documented as broken
Breaking changes:
- remove or rename exported symbol
- change required parameters
- change return type or error type
- change default behavior
- remove config key
- move public import path without compatibility shim
## Deprecation Policy
Default policy:
- Introduce replacement first.
- Mark old API deprecated in a minor release.
- Emit deprecation warning where the ecosystem supports it.
- Keep deprecated API for at least one major line.
- Remove only in the next major release.
Deprecation notice should include:
- deprecated symbol
- replacement API
- first deprecated version
- planned removal version
- migration example or link
Example:
```text
Deprecated since 2.4.0: use Client.fetch_item() instead.
Planned removal: 3.0.0.
```
Do not deprecate without a working replacement unless the API is unsafe or unusable.
## Migration Guides
Every breaking change needs BEFORE / AFTER examples.
```text
BEFORE
client = create_client(token)
result = client.fetch(id)
AFTER
client = Client(token=token)
result = client.fetch_item(id)
```
Migration guide contents:
- affected versions
- who is affected
- exact change
- mechanical replacement when possible
- behavior differences
- rollout order for large applications
- known edge cases
Store guides close to releases, such as `docs/migrations/v2-to-v3.md`.
## Dependency Policy
Principles:
- minimize transitive dependencies
- avoid dependencies for small utilities
- avoid framework lock-in in core modules
- isolate optional integrations
- document supported dependency version ranges
Runtime dependencies:
- Prefer standard library when practical.
- Avoid large frameworks in a small library.
- Avoid exposing dependency types in public API unless intentional.
- Track security updates.
Peer dependencies for npm-style ecosystems:
- Use peer dependencies when the host app must provide the framework.
- Do not bundle a second copy of React, Vue, ESLint, or similar host-level packages.
- State supported peer ranges.
- Test lowest and latest supported peer versions when possible.
Pin vs range:
| Context | Policy |
|---------|--------|
| application lockfile | pin exact resolved versions |
| published library manifest | use compatible ranges |
| security-sensitive tool | narrow range or pin with automation |
| peer dependency | supported range |
## Documentation Contract
Every exported symbol should have docs:
- docstring / JSDoc / KDoc / Rustdoc / Go doc
- parameter meaning
- return meaning
- error behavior
- side effects
- lifecycle or concurrency notes when relevant
README minimum:
- what the library does
- supported platforms or runtimes
- installation
- quick-start
- common configuration
- link to API docs
- versioning and compatibility note
Examples directory:
```
examples/
├── quick-start/
├── custom-config/
└── error-handling/
```
Example rules:
- runnable from a clean checkout
- minimal but complete
- covers one major feature each
- included in CI when practical
## Testing
Test from the package boundary:
```
import { publicFunction } from "package-name"
```
or equivalent public import path.
Test levels:
- public API behavior tests
- compatibility tests for documented edge cases
- error contract tests
- example smoke tests
- packaging test that installs the built artifact locally
Rules:
- Public boundary tests must exist.
- Internal tests are allowed for complex algorithms.
- Add regression tests before changing behavior.
- Test deprecation warnings if consumers rely on them.
## Packaging
Document build and publish commands.
Common ecosystems:
- npm: `npm pack`, then publish to npm registry.
- PyPI: build wheel and sdist, then publish.
- crates.io: `cargo package`, `cargo publish`.
- Maven Central: signed group, artifact, version coordinates.
- Go: tag module versions; consumers fetch from VCS or module proxy.
Packaging checklist:
- package includes README, license, and changelog
- package excludes tests or fixtures unless needed
- source maps or type declarations included when relevant
- artifact installs in a clean temp project
- published version matches changelog and git tag
- license metadata is correct
## Versioning Automation
Useful tools:
- changesets
- conventional commits
- semantic-release
- release-please
- language-native release tooling
Rules:
- Automation should not guess breaking changes silently.
- Human review is required for major releases.
- Changelog entries should describe consumer impact.
- Release tags should match package versions.
## Folder Layout per Phase
```
src/{package}/
├── index.*          # public entrypoint / exports             (Phase 1)
├── public/          # optional public facade modules          (Phase 1)
├── internal/        # implementation details                  (Phase 1)
├── integrations/    # optional adapters                       (Phase 2)
├── errors.*         # stable error types / codes              (Phase 1)
└── config.*         # documented configuration model          (Phase 1)
docs/
├── api.md
└── migrations/
examples/
└── quick-start/
```
Phase rule:
- Phase 1: public API, internal core, tests from package boundary.
- Phase 2: docs, examples, optional integrations, packaging smoke test.
- Phase 3: release automation, compatibility matrix, migration guides.
## Common Anti-patterns
- Public API grows rapidly before 1.0 without stability labels.
- Internal classes are accidentally exported from the package root.
- Consumers must import from deep internal paths.
- Hard-coded URLs, paths, credentials, or region names.
- Undocumented config keys change behavior.
- Public API exposes dependency-specific types unintentionally.
- Breaking changes ship as patch or minor versions.
- Deprecation has no replacement or removal schedule.
- README quick-start does not run.
- Tests import private internals while public imports are broken.
- Package contents differ from repository assumptions.
- Release automation publishes without changelog review.
