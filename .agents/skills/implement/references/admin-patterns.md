# Admin API Controller Patterns

## Architecture

```
admin-api (Kotlin) -> mono (Java)
├── Controller (presentation) ─┬-> Service (business logic)
│                              ├-> Repository (JPA + QueryDSL)
│                              └-> DTO (Request/Response)
```

**Core principles:**
- admin-api handles presentation layer only (Kotlin)
- Business logic and DTOs are written in mono module (Java)
- admin-api depends on `spring-data-jpa` only as `testImplementation`

## Controller Rules

```kotlin
// Location: bottlenote-admin-api/src/main/kotlin/app/bottlenote/{domain}/presentation/Admin{Domain}Controller.kt
@RestController
@RequestMapping("/{plural-resources}")
class Admin{Domain}Controller(
    private val service: Admin{Domain}Service
) {
    // List (search) - service returns GlobalResponse with pagination
    @GetMapping
    fun search(@ModelAttribute request: Admin{Domain}SearchRequest): ResponseEntity<*> {
        return ResponseEntity.ok(service.search(request))
    }

    // Detail
    @GetMapping("/{id}")
    fun getDetail(@PathVariable id: Long): ResponseEntity<*> {
        return GlobalResponse.ok(service.getDetail(id))
    }

    // Create
    @PostMapping
    fun create(@RequestBody @Valid request: Admin{Domain}CreateRequest): ResponseEntity<*> {
        val adminId = SecurityContextUtil.getAdminUserIdByContext()
            .orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
        return GlobalResponse.ok(service.create(request, adminId))
    }
}
```

## Key Rules

| Rule | Detail |
|------|--------|
| **Package** | `app.bottlenote.{domain}.presentation` |
| **Class name** | `Admin{Domain}Controller` |
| **Mapping** | `@RequestMapping("/{plural-resources}")` (context-path `/admin/api/v1` is configured) |
| **Response (list)** | `ResponseEntity.ok(service.search(request))` — service returns `GlobalResponse` |
| **Response (other)** | `GlobalResponse.ok(service.method())` |
| **Auth** | `SecurityContextUtil.getAdminUserIdByContext()` |
| **RBAC** | Roles: `ROOT_ADMIN`, `PARTNER`, `COMMUNITY_MANAGER` |

## Service Rules

| Rule | Detail |
|------|--------|
| **Class name** | `Admin{Domain}Service` in mono module |
| **List query return** | `GlobalResponse.fromPage(page)` — NOT `Page<T>` directly |
| **Detail return** | Response DTO — service handles conversion (no `from(Entity)` on DTO) |
| **CUD return** | `AdminResultResponse.of(ResultCode, targetId)` |
| **Read transactions** | `@Transactional(readOnly = true)` |
| **Write transactions** | `@Transactional` |

## DTO Rules

| Rule | Detail |
|------|--------|
| **DTO-Entity separation** | Response DTOs must NOT reference Entity directly (architecture rule violation) |
| **Conversion** | Use `of(...)` factory method or direct constructor in Service. `from(Entity)` is prohibited |
| **Format** | Java `record` with `@Builder` constructor for defaults |
| **Validation** | `@NotBlank`, `@NotNull`, etc. via Bean Validation |

## Authentication

```kotlin
val adminId = SecurityContextUtil.getAdminUserIdByContext()
    .orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
```

## Pagination

Admin uses **offset pagination** (vs. product's cursor pagination):
- Parameters: `page`, `size`
- Spring Data's `Pageable` interface
- Service returns `GlobalResponse.fromPage(page)`

## HTTP Method Conventions

| Action | Method | URL Pattern | Example |
|--------|--------|-------------|---------|
| List/Search | GET | `/{resources}` | `GET /curations` |
| Detail | GET | `/{resources}/{id}` | `GET /curations/1` |
| Create | POST | `/{resources}` | `POST /curations` |
| Full update | PUT | `/{resources}/{id}` | `PUT /curations/1` |
| Partial update | PATCH | `/{resources}/{id}/{field}` | `PATCH /curations/1/status` |
| Delete | DELETE | `/{resources}/{id}` | `DELETE /curations/1` |
| Sub-resource add | POST | `/{resources}/{id}/{sub}` | `POST /curations/1/alcohols` |
| Sub-resource remove | DELETE | `/{resources}/{id}/{sub}/{subId}` | `DELETE /curations/1/alcohols/5` |

## Exception Pattern

```
RuntimeException
  -> AbstractCustomException (ExceptionCode)
    -> {Domain}Exception ({Domain}ExceptionCode)
```

- `{Domain}ExceptionCode`: implements `ExceptionCode` interface (`getMessage()`, `getHttpStatus()`)
- `{Domain}Exception`: extends `AbstractCustomException`

## Reference Implementations

| Feature | Controller | Service | Test |
|---------|-----------|---------|------|
| **Curation** (sub-resource mgmt) | `admin-api/.../alcohols/presentation/AdminCurationController.kt` | `mono/.../alcohols/service/AdminCurationService.java` | `admin-api/.../integration/curation/AdminCurationIntegrationTest.kt` |
| **Banner** (QueryDSL, validation) | `admin-api/.../banner/presentation/AdminBannerController.kt` | `mono/.../banner/service/AdminBannerService.java` | `admin-api/.../integration/banner/AdminBannerIntegrationTest.kt` |
