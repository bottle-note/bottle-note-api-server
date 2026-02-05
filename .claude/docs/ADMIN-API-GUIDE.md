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
| 1 | Request/Response DTO | mono | `{domain}/dto/request/`, `{domain}/dto/response/` |
| 2 | ExceptionCode 추가 | mono | `{domain}/exception/{Domain}ExceptionCode.java` |
| 3 | ResultCode 추가 | mono | `global/dto/response/AdminResultResponse.java` |
| 4 | Repository 확장 (필요 시) | mono | `{domain}/repository/` |
| 5 | Service 작성 | mono | `{domain}/service/Admin{Domain}Service.java` |
| 6 | Controller 작성 | admin-api | `{domain}/presentation/Admin{Domain}Controller.kt` |

### Phase 2: 테스트

| 순서 | 작업 | 위치 |
|------|------|------|
| 1 | Test Helper | `app/helper/{domain}/{Domain}Helper.kt` (object 싱글톤) |
| 2 | Integration Test | `app/integration/{domain}/Admin{Domain}IntegrationTest.kt` |

### Phase 3: 문서화 (선택)

| 순서 | 작업 | 위치 |
|------|------|------|
| 1 | RestDocs Test | `app/docs/{domain}/Admin{Domain}ControllerDocsTest.kt` |
| 2 | AsciiDoc | `src/docs/asciidoc/api/{domain}/` |

---

## 핵심 규칙

### DTO 규칙

| 규칙 | 설명 |
|------|------|
| **DTO-Entity 분리** | Response DTO는 Entity를 직접 참조하면 안 됨 (아키텍처 규칙 위반) |
| **팩토리 메서드** | `from(Entity)` 금지 → `of(...)` 사용, 변환 로직은 Service에서 처리 |
| **record 사용** | Java record로 작성, `@Builder` 생성자에서 기본값 설정 |
| **Validation** | `@NotBlank`, `@NotNull` 등 Bean Validation 사용 |

### Service 규칙

| 규칙 | 설명 |
|------|------|
| **목록 조회 반환** | `Page<T>` 직접 반환 금지 → `GlobalResponse.fromPage()` 사용 |
| **상세 조회 반환** | Response DTO 반환, `of()` 팩토리로 변환 |
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

| 항목 | 파일 경로 |
|------|----------|
| Controller | `admin-api/.../alcohols/presentation/AdminCurationController.kt` |
| Service | `mono/.../alcohols/service/AdminCurationService.java` |
| Response DTO | `mono/.../alcohols/dto/response/AdminCurationDetailResponse.java` |
| Request DTO | `mono/.../alcohols/dto/request/AdminCuration*Request.java` |
| Integration Test | `admin-api/.../integration/curation/AdminCurationIntegrationTest.kt` |
| RestDocs Test | `admin-api/.../docs/curation/AdminCurationControllerDocsTest.kt` |
| Helper | `admin-api/.../helper/curation/CurationHelper.kt` |

---

## 검증 절차

Admin API 구현 완료 후 아래 순서대로 검증:

| 순서 | 검증 항목 | 명령어 | 태그/범위 |
|------|----------|--------|-----------|
| 1 | 컴파일 | `./gradlew :bottlenote-admin-api:compileKotlin` | - |
| 2 | 코드 포맷팅 | `./gradlew :bottlenote-mono:spotlessCheck` | mono만 적용 |
| 3 | 아키텍처 규칙 | `./gradlew :bottlenote-mono:check_rule_test` | `@Tag("rule")` |
| 4 | 단위 테스트 | `./gradlew unit_test` | `@Tag("unit")` |
| 5 | 어드민 통합 테스트 | `./gradlew admin_integration_test` | `@Tag("admin_integration")` |
| 6 | REST Docs 생성 | `./gradlew :bottlenote-admin-api:restDocsTest` | `app.docs.*` |

**전체 검증 (위 1-5 포함):**
```bash
./gradlew :bottlenote-admin-api:build
```
