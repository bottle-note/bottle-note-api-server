# Product API Controller Patterns

## Controller Structure

```java
// Location: bottlenote-product-api/src/main/java/app/bottlenote/{domain}/controller/{Domain}Controller.java
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/{domain}")
public class {Domain}Controller {

    private final {Domain}CommandService commandService;
    private final {Domain}QueryService queryService;

    @GetMapping
    public ResponseEntity<?> list(@ModelAttribute PageableRequest request) {
        Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
        // ...
        return GlobalResponse.ok(response, metaInfos);
    }

    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody @Valid CreateRequest request
    ) {
        Long userId = SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));
        return GlobalResponse.ok(commandService.create(request, userId));
    }
}
```

## Key Rules

| Rule | Detail |
|------|--------|
| **Path** | `@RequestMapping("/api/v1/{plural-resource}")` |
| **Auth required** | `SecurityContextUtil.getUserIdByContext().orElseThrow(...)` |
| **Auth optional** | `SecurityContextUtil.getUserIdByContext().orElse(-1L)` |
| **Response** | Always wrap with `GlobalResponse.ok()` |
| **Pagination** | `CursorPageable` + `PageResponse` + `MetaInfos` |
| **Validation** | `@RequestBody @Valid` for POST/PUT/PATCH |
| **Query params** | `@ModelAttribute` for GET list endpoints |

## Pagination Pattern

```java
// In controller
MetaInfos metaInfos = MetaService.createMetaInfo();
metaInfos.add("pageable", response.cursorPageable());
metaInfos.add("searchParameters", request);
return GlobalResponse.ok(response, metaInfos);
```

## Endpoint URL Patterns

| HTTP Method | Purpose | URL Pattern | Example |
|-------------|---------|-------------|---------|
| GET | List | `/api/v1/{resources}` | `GET /api/v1/reviews` |
| GET | Detail | `/api/v1/{resources}/{id}` | `GET /api/v1/reviews/1` |
| POST | Create | `/api/v1/{resources}` | `POST /api/v1/reviews` |
| PUT | Full update | `/api/v1/{resources}/{id}` | `PUT /api/v1/reviews/1` |
| PATCH | Partial | `/api/v1/{resources}/{id}` | `PATCH /api/v1/reviews/1` |
| DELETE | Delete | `/api/v1/{resources}/{id}` | `DELETE /api/v1/reviews/1` |
