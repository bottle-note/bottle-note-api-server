# Type: web-api

Language-independent patterns for HTTP-based API servers (REST primarily; GraphQL notes at the end). Pair with the matching `languages/{language}.md` for concrete code.

## Layer Breakdown (universal)

```
HTTP request
    ↓
[Surface]          controller / route / handler        — thin, parses request, calls service
    ↓
[Service]          service / use case / interactor     — business logic, orchestration
    ↓
[Repository]       data access                          — persistence boundary
    ↓
[Domain model]     entities / value objects             — core types
```

Rules:
- **Surface is thin** — parse / validate inputs, call service, format response. No business logic.
- **Cross-module access** goes through a `Facade` / port / public function — never direct repository injection from another domain.
- **Domain model knows nothing about HTTP** — it should be unit-testable without any framework.

## DTO Conventions

### Request DTO

- Immutable (record / dataclass / struct with no setters)
- Validation at the boundary (Bean Validation / Pydantic / `validator` tags)
- Reject extra fields if the framework supports it (Pydantic v2 `model_config = ConfigDict(extra="forbid")`, Java `@JsonIgnoreProperties(ignoreUnknown = false)`)
- Defaults at the DTO level (pagination size, sort), not in service

### Response DTO

- Separate type from Domain model — DTO must NOT reference Entity directly
- Conversion via factory (`of(...)` / `from_domain(...)`) in service layer
- Stable shape across releases (additive only) — see Versioning below

## Endpoint Design

### HTTP Method × URL Pattern

| Method | Purpose | URL Pattern | Body | Idempotent |
|--------|---------|-------------|------|------------|
| GET | List | `/{resources}` | none | yes |
| GET | Detail | `/{resources}/{id}` | none | yes |
| POST | Create | `/{resources}` | request DTO | no |
| PUT | Full update / replace | `/{resources}/{id}` | full DTO | yes |
| PATCH | Partial update | `/{resources}/{id}` | partial DTO | yes |
| DELETE | Delete | `/{resources}/{id}` | none | yes |

Sub-resources:

| Method | URL Pattern | Example |
|--------|-------------|---------|
| POST | `/{resources}/{id}/{sub}` | `POST /curations/1/items` |
| DELETE | `/{resources}/{id}/{sub}/{subId}` | `DELETE /curations/1/items/5` |
| PATCH | `/{resources}/{id}/{field}` | `PATCH /curations/1/status` |

### Status Code Conventions

| Code | Use |
|------|-----|
| 200 | Successful GET / PUT / PATCH / DELETE with body |
| 201 | Successful POST creating a new resource |
| 204 | Successful operation with no body (rare for APIs) |
| 400 | Client-side validation failure |
| 401 | Missing / invalid authentication |
| 403 | Authenticated but not authorized for this resource |
| 404 | Resource not found |
| 409 | Conflict (duplicate, version mismatch) |
| 422 | Request structurally valid but semantically wrong (some teams use 400 instead) |
| 429 | Rate limit |
| 5xx | Server-side failure (never leak internals in body) |

Pick a project convention for 400 vs 422 and stick to it; document in `conventions.md`.

## Error Model

### Shape

```json
{
  "success": false,
  "code": "RATING_NOT_FOUND",
  "message": "rating not found",
  "errors": [
    { "field": "rating", "reason": "must be between 0 and 5" }
  ],
  "meta": { "requestId": "...", "timestamp": "..." }
}
```

- `code`: enum-like stable string (machine-readable)
- `message`: human-readable, may localize but keep `code` stable
- `errors`: field-level details for 400 / 422
- `meta`: tracing aids

### Implementation

- Domain-specific exception classes (`{Domain}Exception` + `{Domain}ExceptionCode` enum)
- Global handler at the framework boundary (Spring `@RestControllerAdvice`, FastAPI `exception_handler`, Go middleware)
- Never leak stack traces or framework internals to the client
- Log internally with correlation ID, return the same correlation ID to the client (`requestId` in `meta`)

## Auth Integration

### Where auth enters

```
HTTP request
    ↓
[Auth middleware / filter]                              — verifies token, populates principal
    ↓
[Surface]    SecurityContextUtil.getUserId() / request.user / ctx.Value()
    ↓
[Service]    receives userId / userRole as a parameter — NOT from a static context
```

**Rule**: services receive auth principal as an explicit parameter. They should not pull from a thread-local / request-scoped global — that makes services untestable in pure unit tests.

### Surface-layer extraction

| Framework | Auth principal extraction |
|-----------|---------------------------|
| Spring | `SecurityContextUtil.getUserIdByContext().orElseThrow(...)` |
| FastAPI | `user: User = Depends(get_current_user)` |
| Go | `user := middleware.UserFromContext(r.Context())` |

### Optional auth (public-readable endpoints)

Distinguish "auth optional" (returns -1L / None / empty principal when missing) from "auth required" (rejects 401). Document both clearly.

## Pagination

### Cursor (preferred for public APIs)

Stable under concurrent inserts, infinite scroll friendly:

```
Request:  cursor=<last_id>, pageSize=20
Response: items[20], nextCursor=<new_last_id>, hasNext=true
```

Implementation tip: query `limit = pageSize + 1`, drop the extra item, use its ID as next cursor.

### Offset (preferred for admin UIs)

Page-jump UX requires it:

```
Request:  page=3, size=20
Response: items[20], totalElements, totalPages, page, size
```

Tradeoff: offset becomes inconsistent under concurrent inserts (skip / duplicate rows). Acceptable for admin scenarios where consistency is less critical than UI capability.

### Mixed-mode policy

If the project has both public and admin surfaces:
- Public: cursor, returns `pageable: { cursor, hasNext, pageSize }` in `meta`
- Admin: offset, returns `pageable: { page, size, totalElements, totalPages }` or `GlobalResponse.fromPage(page)` equivalent

## Async / Events

### Domain events

- Publish from the service that owns the aggregate (`publisher.publishEvent(...)`)
- Subscribe with framework-specific listener annotation (`@TransactionalEventListener` + `@Async` in Spring, FastAPI background tasks, Go channel + worker)
- Listener runs in **separate transaction** (`REQUIRES_NEW` or equivalent) so its failure does NOT roll back the main transaction

### When to publish

- After persistence success (post-commit), not in the middle of a multi-step transaction
- Idempotently — listeners may re-run on retry

### Cross-service events

- Internal monolith: in-process event bus
- Cross-service: message queue (Kafka, SQS, RabbitMQ) — but that's deployment-level, beyond a single web-api project

## Versioning

### URL versioning (recommended for major changes)

```
/api/v1/ratings
/api/v2/ratings   ← breaking change in shape
```

### Header versioning (alternative)

```
Accept: application/vnd.bottlenote.v1+json
```

### Backward-compatible changes (no version bump)

- Add new optional fields to request — clients ignore
- Add new fields to response — clients ignore (only if clients use additive parsers)
- Add new endpoints

### Breaking changes (new major version)

- Remove field
- Rename field
- Change type of existing field
- Change semantics of existing endpoint

## Idempotency

- GET, PUT, DELETE: naturally idempotent
- POST: optionally accept `Idempotency-Key` header — first request executes, subsequent same-key requests return the same response (for payment / critical flows)
- PATCH: should be idempotent in practice but framework doesn't enforce

## Folder Layout per Phase (cross-language template)

```
src/{root}/{domain}/
├── domain/          # entities, domain repository interfaces (Phase 1 mono)
├── dto/
│   ├── request/     # request DTOs                     (Phase 1)
│   └── response/    # response DTOs                    (Phase 1)
├── exception/       # domain exceptions                (Phase 1)
├── repository/      # repository implementations       (Phase 1)
├── service/         # service / use case               (Phase 1)
├── facade/          # cross-aggregate seam             (Phase 1, when needed)
└── (controller / route / handler)                      (Phase 2 — surface)
```

The `controller / route / handler` location is language-specific:
- Java/Spring: same `{domain}/controller/` package
- Python/FastAPI: `{domain}/router.py`
- Go: `internal/{domain}/handler/`

## Common Anti-patterns

- Business logic in the surface layer (route/controller has `if`s about domain state — push down to service)
- Service depends on framework-specific request object (depend on plain types: `userId: long`, not `HttpServletRequest`)
- DTO references Entity (couples API shape to persistence — versioning becomes impossible)
- Status code overloading (`200 { success: false }`) — use proper HTTP codes
- Leaking persistence errors as HTTP responses ("duplicate key value violates unique constraint" → never)
- Unbounded list endpoints (no pagination, returns all rows)
- N+1 in list endpoints (lazy-loading associations per row — use fetch joins or projection)
- Auth principal extracted from a global thread-local inside service (untestable)
- Versioned URLs without a stated breakage policy
- Generic 500 for all errors (forces clients to parse messages)

## GraphQL Notes (when used)

- Schema-first or code-first — pick one and stick to it
- Single endpoint (`POST /graphql`), so versioning happens at the schema field level (additive only)
- N+1 is the default failure mode — use DataLoader pattern
- Auth integration via context (resolver `info.context`)
- Tooling: same project conventions for naming + error shape as REST
