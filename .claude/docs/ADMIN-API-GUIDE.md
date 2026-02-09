# Admin API 구현 가이드

> Admin API 구현 시 따라야 할 규칙과 체크리스트를 정의합니다.
> 코드 예시는 기존 구현 파일을 참고하세요.

---

## 아키텍처 개요

```
admin-api (Kotlin) → mono (Java)
├── Controller (presentation) ─┬→ Service (비즈니스 로직)
│                              ├→ Repository (JPA + QueryDSL)
│                              └→ DTO (Request/Response)
```

**핵심 원칙:**
- admin-api는 프레젠테이션 계층만 담당 (Kotlin)
- 비즈니스 로직과 DTO는 mono 모듈에 작성 (Java)
- admin-api는 `spring-data-jpa`를 `testImplementation`으로만 의존

---

## 구현 체크리스트

### Phase 1: 구현 (mono → admin-api 순서)

| 순서 | 작업 | 모듈 | 위치 |
|------|------|------|------|
| 1 | 엔티티 수정 메서드 추가 (필요 시) | mono | `{domain}/domain/{Domain}.java` |
| 2 | Repository 확장 (필요 시) | mono | `{domain}/repository/` |
| 3 | Request/Response DTO | mono | `{domain}/dto/request/`, `{domain}/dto/response/` |
| 4 | ExceptionCode + Exception 추가 | mono | `{domain}/exception/{Domain}ExceptionCode.java`, `{Domain}Exception.java` |
| 5 | ResultCode 추가 | mono | `global/dto/response/AdminResultResponse.java` |
| 6 | Service 작성 | mono | `{domain}/service/Admin{Domain}Service.java` |
| 7 | Controller 작성 | admin-api | `{domain}/presentation/Admin{Domain}Controller.kt` |

> [주의] Repository 인터페이스에 메서드를 추가하면 `InMemory{Domain}Repository` 테스트 픽스처도 반드시 동기화해야 한다.

### Phase 2: 테스트

| 순서 | 작업 | 위치 |
|------|------|------|
| 1 | Test Helper | `app/helper/{domain}/{Domain}Helper.kt` (object 싱글톤) |
| 2 | Integration Test | `app/integration/{domain}/Admin{Domain}IntegrationTest.kt` |

### Phase 3: 문서화 (선택)

| 순서 | 작업 | 위치 |
|------|------|------|
| 1 | RestDocs Test | `app/docs/{domain}/Admin{Domain}ControllerDocsTest.kt` |
| 2 | Enum adoc 생성 | `src/docs/asciidoc/api/common/enums/{enum-name}.adoc` |
| 3 | Domain adoc 생성 | `src/docs/asciidoc/api/admin-{domain}/{domain}.adoc` |
| 4 | 메인 문서에 include 추가 | `src/docs/asciidoc/admin-api.adoc` |
| 5 | asciidoctor 빌드 검증 | `./gradlew :bottlenote-admin-api:asciidoctor` |

> Domain adoc에서 enum adoc을 include할 때 상대 경로 사용: `include::../common/enums/{enum-name}.adoc[]`

---

## 핵심 규칙

### DTO 규칙

| 규칙 | 설명 |
|------|------|
| **DTO-Entity 분리** | Response DTO는 Entity를 직접 참조하면 안 됨 (아키텍처 규칙 위반) |
| **변환 로직** | `from(Entity)` 금지 → Service에서 직접 생성자 호출 또는 `of(...)` 팩토리 사용 |
| **record 사용** | Java record로 작성, `@Builder` 생성자에서 기본값 설정 |
| **Validation** | `@NotBlank`, `@NotNull` 등 Bean Validation 사용 |

### Service 규칙

| 규칙 | 설명 |
|------|------|
| **목록 조회 반환** | `Page<T>` 직접 반환 금지 → `GlobalResponse.fromPage()` 사용 |
| **상세 조회 반환** | Response DTO 반환, Service에서 변환 (직접 생성자 또는 `of()`) |
| **CUD 반환** | `AdminResultResponse.of(ResultCode, targetId)` 통일 |
| **트랜잭션** | 조회는 `@Transactional(readOnly = true)`, CUD는 `@Transactional` |

### Controller 규칙

| 규칙 | 설명 |
|------|------|
| **목록 조회** | `ResponseEntity.ok(service.search(request))` (Service가 GlobalResponse 반환) |
| **그 외 API** | `GlobalResponse.ok(service.xxx())` |
| **매핑** | `@RequestMapping("/{복수형}")` (kebab-case) |
| **검색 파라미터** | `@ModelAttribute` 사용 |
| **Body 파라미터** | `@RequestBody @Valid` 사용 |

### 페이징 방식

| API 유형 | 방식 | 파라미터 |
|----------|------|----------|
| Admin (관리자) | 오프셋 페이징 | `page`, `size` |
| Product (클라이언트) | 커서 페이징 | `cursor`, `pageSize` |

---

## HTTP 메서드 규칙

| 작업 | Method | URL 패턴 | 예시 |
|------|--------|----------|------|
| 목록 조회 | GET | `/{resources}` | `GET /curations` |
| 상세 조회 | GET | `/{resources}/{id}` | `GET /curations/1` |
| 생성 | POST | `/{resources}` | `POST /curations` |
| 전체 수정 | PUT | `/{resources}/{id}` | `PUT /curations/1` |
| 부분 수정 | PATCH | `/{resources}/{id}/{field}` | `PATCH /curations/1/status` |
| 삭제 | DELETE | `/{resources}/{id}` | `DELETE /curations/1` |
| 하위 리소스 추가 | POST | `/{resources}/{id}/{sub}` | `POST /curations/1/alcohols` |
| 하위 리소스 삭제 | DELETE | `/{resources}/{id}/{sub}/{subId}` | `DELETE /curations/1/alcohols/5` |

---

## 예외 계층 구조

```
RuntimeException
  → AbstractCustomException (ExceptionCode 보유)
    → {Domain}Exception ({Domain}ExceptionCode)
```

- `{Domain}ExceptionCode`: `ExceptionCode` 인터페이스 구현 (`getMessage()`, `getHttpStatus()`)
- `{Domain}Exception`: `AbstractCustomException` 상속, 생성자에서 `ExceptionCode` 전달
- 참고: `AlcoholExceptionCode.java`, `BannerExceptionCode.java`

---

## 테스트 규칙

### Integration Test

| 항목 | 규칙 |
|------|------|
| 상속 | `IntegrationTestSupport` |
| 태그 | `@Tag("admin_integration")` |
| 인증 | `getAccessToken(admin)` → `Authorization: Bearer $token` |
| 검증 | `mockMvcTester.get/post/put/delete()` + AssertJ |

### 필수 테스트 케이스

| API | 필수 테스트 |
|-----|-----------|
| 목록 조회 | 성공, 인증 실패, 필터링 |
| 상세 조회 | 성공, 인증 실패, 존재하지 않는 ID |
| 생성 | 성공, 인증 실패, 필수 필드 누락, 중복 검사 |
| 수정 | 성공, 인증 실패, 존재하지 않는 ID |
| 삭제 | 성공, 인증 실패, 존재하지 않는 ID |

### RestDocs Test

| 항목 | 규칙 |
|------|------|
| 어노테이션 | `@WebMvcTest(excludeAutoConfiguration = [SecurityAutoConfiguration::class])` |
| Mock | `@MockitoBean`으로 Service 목킹 |
| 목록 조회 Mock | `GlobalResponse.fromPage(page)` 반환 |
| 그 외 Mock | Response DTO 또는 `AdminResultResponse` 반환 |

---

## 참고 구현 파일

### Curation (큐레이션) - 하위 리소스 관리 포함

| 항목 | 파일 경로 |
|------|----------|
| Controller | `admin-api/.../alcohols/presentation/AdminCurationController.kt` |
| Service | `mono/.../alcohols/service/AdminCurationService.java` |
| Response DTO | `mono/.../alcohols/dto/response/AdminCurationDetailResponse.java` |
| Request DTO | `mono/.../alcohols/dto/request/AdminCuration*Request.java` |
| Integration Test | `admin-api/.../integration/curation/AdminCurationIntegrationTest.kt` |
| RestDocs Test | `admin-api/.../docs/curation/AdminCurationControllerDocsTest.kt` |
| Helper | `admin-api/.../helper/curation/CurationHelper.kt` |

### Banner (배너) - QueryDSL 검색, 비즈니스 검증, sortOrder 리오더링 포함

| 항목 | 파일 경로 |
|------|----------|
| Controller | `admin-api/.../banner/presentation/AdminBannerController.kt` |
| Service | `mono/.../banner/service/AdminBannerService.java` |
| Response DTO | `mono/.../banner/dto/response/AdminBanner*Response.java` |
| Request DTO | `mono/.../banner/dto/request/AdminBanner*Request.java` |
| Exception | `mono/.../banner/exception/BannerException.java`, `BannerExceptionCode.java` |
| QueryDSL | `mono/.../banner/repository/CustomBannerRepository*.java` |
| Integration Test | `admin-api/.../integration/banner/AdminBannerIntegrationTest.kt` |
| RestDocs Test | `admin-api/.../docs/banner/AdminBannerControllerDocsTest.kt` |
| Helper | `admin-api/.../helper/banner/BannerHelper.kt` |
| AsciiDoc | `admin-api/src/docs/asciidoc/api/admin-banners/banners.adoc` |
| Enum adoc | `admin-api/src/docs/asciidoc/api/common/enums/banner-type.adoc`, `text-position.adoc` |

---

## 검증 절차

Admin API 구현 완료 후 아래 순서대로 검증:

| 순서 | 검증 항목 | 명령어 | 태그/범위 |
|------|----------|--------|-----------|
| 1 | 컴파일 | `./gradlew :bottlenote-admin-api:compileKotlin` | - |
| 2 | 코드 포맷팅 | `./gradlew :bottlenote-mono:spotlessCheck` | mono만 적용 |
| 3 | 아키텍처 규칙 | `./gradlew check_rule_test` | `@Tag("rule")` |
| 4 | 단위 테스트 | `./gradlew unit_test` | `@Tag("unit")` |
| 5 | Product 통합 테스트 | `./gradlew integration_test` | `@Tag("integration")` |
| 6 | Admin 통합 테스트 | `./gradlew admin_integration_test` | `@Tag("admin_integration")` |
| 7 | REST Docs 빌드 | `./gradlew :bottlenote-admin-api:asciidoctor` | 문서화 시 |

> CI 파이프라인은 3~6번을 병렬 실행한다. mono 모듈 변경이 product-api에 영향을 줄 수 있으므로 `integration_test`도 반드시 확인해야 한다.

**전체 검증 (위 1-6 포함):**
```bash
./gradlew :bottlenote-admin-api:build
```
