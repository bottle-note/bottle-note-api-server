# Plan: OpenAPI 어노테이션 기반 API 문서화 (product-api)

## Overview

**무엇을**: product-api에 springdoc-openapi를 도입하고, 26개 컨트롤러 71개 endpoint에 문서 어노테이션을 부여해 `GET /openapi.product.json`으로 OpenAPI 3.1 스펙을 제공한다.

**왜**: 현재 문서 체계는 RestDocs 테스트 41개 → asciidoc 92개 → Antora 정적 사이트다. 세 단계를 사람이 수동으로 동기화해야 하고 산출물이 HTML이라 기계가 읽을 수 없다. OpenAPI 스펙으로 바꾸면 클라이언트 코드 생성, Scalar UI, API 테스트 도구를 그 위에 붙일 수 있다.

**방식 결정 배경**: main에는 이미 `restdocs-api-spec` 0.19.4 플러그인이 양쪽 API 모듈에 적용되어 있고 파일럿 테스트 1개(`OpenApiAuthV2ControllerTest`)가 존재한다. 즉 "테스트 기반 OpenAPI" 경로가 절반쯤 깔려 있었으나, 이 경로는 endpoint마다 문서화 테스트를 유지해야 하고 Mockito 의존(레이어 표준 10 위반)이라 확장을 포기했다. 대신 런타임 어노테이션 방식(springdoc)으로 통일한다.

### Assumptions

**범위**

1. 대상은 **product-api만**. admin-api(13개 컨트롤러, Kotlin)는 이번 범위 밖 — product에서 패턴을 확정한 뒤 후속 작업으로 분리한다.
2. `springdoc-openapi-starter-webmvc-ui` **2.8.17을 신규 추가**한다. main에는 springdoc 의존성이 없다. 2.8.x가 Spring Boot 3.x 지원 라인이며 3.0.x는 Boot 4 전용이다.
3. **기존 RestDocs 자산은 제거하지 않는다.** `.adoc` 92개, docs 테스트 41개, Antora `docs/`, `restdocs`·`restdocs-api-spec` 의존성, `asciidoctor`·`openapi3` 태스크를 모두 유지한다. 삭제 예정 표시만 남기고 실제 제거는 후속 작업이다.
4. 따라서 **두 문서화 체계가 일시적으로 공존한다**. 이 중복은 의도된 트레이드오프이며, 전환 기간에 문서 공백을 만들지 않기 위한 선택이다.

**산출물**

5. `GET /openapi.product.json` **런타임 endpoint**로 제공한다. 경로는 `springdoc.api-docs.path`로 지정한다.
6. **swagger-ui는 비활성화**한다. 스펙 JSON만 노출한다.
7. **Scalar UI 도입은 범위 밖**이다. `springdoc-openapi-starter-webmvc-scalar` 2.8.17이 존재하고 에셋을 WebJar로 로컬 서빙하는 것까지 확인했으나, 이번에는 넣지 않는다.
8. 빌드 타임 JSON 파일 산출(`openapi-gradle-plugin`)도 **범위 밖**이다. 런타임 endpoint만 만든다.

**구현 성격**

9. `GlobalResponse.data`는 제네릭이 아닌 `Object` 타입이다. 따라서 응답 스키마는 **springdoc `OperationCustomizer`로 공통 형식 래핑을 적용**하고, 컨트롤러 메서드는 `data`에 들어갈 실제 타입만 힌트로 지정한다.
   - **2026-07-26 수정**: "전역 자동 적용"은 성립하지 않는다. 77개 중 3개(`GET /api/v2/auth/apple/nonce`, `POST /api/v2/auth/apple`, `POST /api/v2/auth/kakao`)는 공통 형식을 거치지 않고 DTO를 그대로 내보낸다. 이들을 감싸면 문서가 실제 응답과 달라진다.
   - 구분 근거는 **컨트롤러가 선언한 반환 타입**이다. `ResponseEntity<NonceResponse>`처럼 본문 타입을 구체적으로 적으면 감싸지 않고, `ResponseEntity<?>`와 `ResponseEntity<GlobalResponse>`는 감싼다. 예외 목록을 코드 밖에 두지 않으므로 코드와 문서가 어긋날 수 없다.
10. 어노테이션은 런타임 동작에 영향을 주지 않으므로 **API 계약과 응답 본문은 변경되지 않는다.**
   - **2026-07-26 수정**: 컨트롤러 시그니처는 **구체화할 수 있다**. `ResponseEntity<?>` → `ResponseEntity<GlobalResponse>`는 제네릭 소거로 바이트코드와 응답이 동일하므로 API 계약을 바꾸지 않는다. `GlobalResponse.ok()` 3종의 반환 타입을 좁혔고 호출부 77곳이 그대로 컴파일되는 것을 확인했다. 가정의 의도(계약 보존)는 유지된다.
11. 문서 어노테이션은 **컨트롤러 밖으로 격리**한다. 수백 줄짜리 `@Operation` 블록이 컨트롤러 본문에 들어가지 않도록 메타 어노테이션으로 묶는다(profanity-api에서 검증된 패턴).
12. 문서 텍스트는 **한국어**로 쓴다.
13. 문서 텍스트는 **Scalar UI 화면에 그대로 노출될 것을 전제로 자연어 문장으로 쓴다.** 식별자를 그대로 옮기지 않는다. 태그는 도메인 이름, summary는 행위를 서술하는 문장, description은 조건·제약·부수효과를 설명하는 완결된 문장으로 쓴다.
14. **에러 응답(4xx/5xx) 문서화는 범위 밖**이다. 정상 응답(2xx) 스키마만 다룬다.
15. `openapi.product.json`은 **무인증 접근을 허용**한다. `SecurityPolicy` fallback이 `REQUIRED_AUTH`이므로 PUBLIC 라우트를 명시해야 한다.

### Success Criteria

생성된 `openapi.product.json`을 기준으로 검증한다.

1. **컨트롤러 클래스명이 태그로 남은 operation이 0건**이다. 스펙에 노출되는 태그 25개 전부 한국어 도메인 태그를 갖는다. (`review-controller` 같은 fallback 소멸)
2. **77개 operation 전부가 `summary`를 갖는다.** 누락 0건. (paths 69개 / operations 77개 — 2026-07-26 실측)
3. **`summary`가 메서드 식별자와 동일한 operation이 0건**이다. (`createReview`가 아니라 `리뷰를 작성한다` 형태)
4. **응답 스키마가 빈 `{"type":"object"}`인 operation이 0건**이다. 공통 형식을 쓰는 엔드포인트는 `data`가 실제 DTO를 `$ref`로 가리키고, 공통 형식을 쓰지 않는 3개 엔드포인트는 응답 본문 자체가 DTO를 가리킨다. (2026-07-26 수정 — 가정 9 참조)
5. `GET /openapi.product.json`이 **무인증 호출에 200을 반환**하고 `info.title`이 지정값과 일치한다.
6. `/swagger-ui/**`가 **200을 반환하지 않는다.**
7. **기존 테스트가 전부 통과한다.** `unit_test`, `integration_test`, `check_rule_test`, 그리고 기존 문서 파이프라인(`restDocsTest`, `asciidoctor`)까지 포함한다. 운영 동작과 기존 문서 체계가 손상되지 않았음을 증명한다.
8. RestDocs 자산의 **삭제 예정 표시가 존재**하고 그 위치가 이 문서에 기록된다.

### Impact Scope

**모듈**
- `bottlenote-product-api` — 주 대상. 의존성, 설정, 컨트롤러 어노테이션, `OperationCustomizer`
- `bottlenote-mono` — `GlobalResponse`가 여기 있다. 전역 커스터마이저 방식이면 수정이 불필요할 수 있으나, DTO에 `@Schema`를 붙이는 범위에 따라 접촉 가능
- `gradle/libs.versions.toml` — springdoc 버전 카탈로그 등록

**변경 없음**
- 스키마 마이그레이션 불필요 (엔티티 변경 없음, Flyway 무관)
- cross-domain 결합 변화 없음 (Facade 신설 불필요)
- 이벤트 발행·수신 변화 없음
- 캐시 정책 변화 없음
- 외부 API 계약 변화 없음

**주의 지점**
- **레이어 표준 5와 충돌 가능**: profanity-api는 문서 어노테이션을 `app.openapi` 한 곳에 모았으나, 이 프로젝트는 "코드는 자기 도메인 패키지에만 둔다"가 표준이다. 배치 위치를 `app.bottlenote.{domain}.openapi`로 할지 별도 패키지로 할지 `/plan`에서 결정해야 한다.
- **ArchUnit 룰**: `ControllerLayerRules`가 컨트롤러 명명·패키지를 검증한다. 어노테이션 타입은 컨트롤러가 아니라 직접 충돌하지 않지만, 신규 패키지 도입 시 `DataTransferObjectRules` 등과의 충돌 여부를 확인해야 한다.
- **테스트 계층**: endpoint 노출·무인증 접근·swagger-ui 차단은 통합 테스트로 검증한다. 태그·summary 누락 여부는 생성된 JSON을 검사하는 방식이 필요하다.

## Execution Mode

- mode: **delegated**
- scope: **plan, implement**
  - 사용자 선언은 "implement"였다. Tasks 없이는 구현이 불가능하므로 전제인 `plan`을 포함해 해석했다.
  - `test`, `verify`, `commit`, `push`, `pr`은 scope 밖이다. 구현이 끝나면 정지하고 보고한다.
  - 단, 구현 중 컴파일 확인과 변경 영향 범위의 기존 테스트 실행은 `implement`의 일부로 수행한다. 검증 없이 완료를 주장하지 않는다는 프로젝트 지침 때문이다.
- stop-conditions:
  1. 가정 붕괴 — 이 문서의 Assumptions를 깨는 발견 시 즉시 정지하고 재개봉 프로토콜을 따른다.
  2. 검증 3회 연속 실패.
  3. scope 밖 행동이 필요해진 시점 (커밋, 푸시, PR 생성, admin-api 착수 등).

## Tasks

분해 기준은 **도메인**이다. "모든 컨트롤러에 태그 붙이기 → 모든 컨트롤러에 summary 붙이기"는 수평 분해이므로 쓰지 않는다. 도메인 하나마다 태그·summary·description·응답 스키마를 한꺼번에 완결한다.

Task 1-3은 기반이다. 이것들이 없으면 어떤 도메인도 문서화를 완결할 수 없으므로 의존성 경계로서 앞세운다.

대상 컨트롤러 26개의 도메인 분포: alcohols 5, user 4, support 4(block/business/help/report), review 3, curation 2, external 2(앱 정보·알림), rating·picks·like·history·banner·파일 업로드 각 1.

### Task 1: springdoc 도입과 스펙 endpoint 노출
- Acceptance:
  - `GET /openapi.product.json`이 무인증 호출에 200과 OpenAPI 3.1 문서를 반환한다
  - `info.title`이 지정한 한국어 서비스명과 일치한다
  - `/swagger-ui/**`가 200을 반환하지 않는다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiDocs*"`
- Files (advisory): `gradle/libs.versions.toml`, `bottlenote-product-api/build.gradle`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `app/global/security/SecurityPolicyConfig.java`, `app/global/config/OpenApiConfig.java`, 신규 통합 테스트 1개
- Depends: 없음
- Size: M (7파일)
- Status: [x] done

### Task 2: GlobalResponse 응답 스키마 전역 래핑
- Acceptance:
  - 임의의 2xx 응답 스키마가 `success`/`code`/`data`/`errors`/`meta` 구조로 나타난다
  - `data`가 빈 `{"type":"object"}`가 아니라 실제 DTO를 `$ref`로 가리킨다
  - 컨트롤러 시그니처는 변경되지 않는다
- Verification: 위 통합 테스트에 응답 스키마 검증 추가 후 동일 명령
- Files (advisory): 신규 `OperationCustomizer` 1개, `OpenApiConfig.java` 등록
- Depends: 1
- Size: S (2파일)
- Status: [x] done
- 검증 시점 조정: Acceptance 2번(`data`가 실제 DTO를 `$ref`로 가리킴)은 엔드포인트가 타입 힌트를 준 뒤에야 관찰할 수 있다. 래핑 구조 자체는 이 Task에서 검증했고, `$ref` 연결은 첫 힌트가 붙는 Task 4에서 확인한다. 계약의 내용은 그대로다.

### Task 3: 스펙 품질 전수 검증 테스트와 미적용 allowlist
- Acceptance:
  - 생성된 스펙을 읽어 ① 컨트롤러 클래스명이 태그로 남은 operation ② `summary` 누락 ③ `summary`가 메서드 식별자와 동일 ④ 빈 응답 스키마를 검출하는 테스트가 존재한다
  - 아직 적용하지 않은 컨트롤러는 allowlist로 예외 처리되어 테스트가 통과한다
  - allowlist에 26개 컨트롤러가 등재된 상태로 시작한다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): 신규 검증 테스트 1개, allowlist 상수
- Depends: 1
- Size: S (2파일)
- Status: [x] done

### Checkpoint: after Tasks 1-3
- [x] 컴파일 통과
- [x] `check_rule_test` 통과 (ArchUnit 룰이 신규 패키지를 거부하지 않음)
- [x] 기존 `restDocsTest`, `asciidoctor` 통과 (가정 3 — 기존 문서 체계 무손상)

### 확정된 문서화 패턴 (Task 4에서 결정 — Task 5 이후 이 형태를 반복한다)

배치는 **도메인별 분산**으로 정했다. profanity-api는 `app.openapi` 한 곳에 모았으나, 이 프로젝트는 도메인 응집을 표준으로 삼고 ArchUnit으로 강제하므로 도메인 안에 둔다. 한 패키지에 모으면 그 패키지가 모든 도메인의 DTO를 import하게 되는 문제도 있다.

1. 위치: `app.bottlenote.{domain}.controller.docs.{Controller이름}ApiDocs` (컨트롤러당 하나. `external`은 `app.external.{name}.presentation.docs`)
2. 형태: `private` 생성자를 둔 `final` 클래스 안에 메타 어노테이션을 모은다
   - `@ApiTag` — `@Target(TYPE)`, 한국어 도메인 태그와 설명
   - endpoint별 `@interface` — `@Target(METHOD)`, `@Operation(summary, description, responses)`
3. 컨트롤러에는 어노테이션 한 줄만 붙인다 (`@ReviewApiDocs.CreateReview`)
4. 응답 `data` 타입은 서비스 메서드의 실제 반환 타입에서 가져온다. 목록은 `array = @ArraySchema(schema = @Schema(implementation = X.class))`
5. `GlobalResponse.ok(Pair, params)`를 쓰는 엔드포인트는 실제 `data`가 `CollectionResponse<T>`(`{totalCount, items}`)다. 제네릭은 어노테이션으로 표현할 수 없으므로 같은 모양의 문서용 `record`를 ApiDocs 안에 `private`으로 선언해 가리킨다
6. 문장은 화면에 노출되는 자연어로 쓴다. summary는 "~한다" 서술형, description은 조건·제약·`meta` 활용법을 설명한다

### Task 4: 어노테이션 배치 패턴 확정과 review 도메인 적용
- Acceptance:
  - 문서 어노테이션이 컨트롤러 본문 밖에 격리되고, 배치 위치가 레이어 표준 5와 충돌하지 않음이 문서에 기록된다
  - review 3개 컨트롤러가 한국어 도메인 태그를 갖는다
  - 해당 endpoint의 summary가 식별자가 아닌 서술 문장이다
  - allowlist에서 review 컨트롤러 3개가 제거된다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): review 문서 어노테이션 1개, `ReviewController.java`, `ReviewExploreController.java`, `ReviewReplyController.java`
- Depends: 1, 2, 3
- Size: S (4파일)
- Status: [x] done

### Task 5: alcohols 도메인 적용
- Acceptance: alcohols 5개 컨트롤러가 태그·summary·응답 스키마를 갖고 allowlist에서 제거된다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): alcohols 문서 어노테이션, `AlcoholExploreController`, `AlcoholPopularQueryController`, `AlcoholQueryController`, `AlcoholReferenceController`, `TastingTagController`
- Depends: 4
- Size: M (6파일)
- Status: [x] done

### Checkpoint: after Tasks 4-5
- [x] 패턴이 두 도메인에서 반복 적용 가능함이 확인됨
- [x] `check_rule_test` 통과

### Task 5A: 응답 본문 타입 전면 선언과 와일드카드 금지 규칙
사용자 결정으로 범위에 추가됐다(2026-07-26). 가정 9 수정의 후속이다 — 반환 타입이 문서 판단의 근거가 되었으므로, 선언을 빠뜨릴 수 있는 여지 자체를 없앤다.

- Acceptance:
  - 모든 `@RestController` public 메서드가 `ResponseEntity`의 본문 타입을 구체적으로 선언한다 (`ResponseEntity<?>` 0건)
  - ArchUnit 규칙이 와일드카드 반환을 검출해 실패시킨다
  - 규칙이 공허하지 않음을 확인한다 (일부러 위반을 만들어 실패를 관찰)
  - 런타임 동작과 기존 테스트에 영향이 없다
- Verification: `./gradlew :bottlenote-product-api:check_rule_test --rerun-tasks`, `integration_test --tests "*OpenApi*"`, `restDocsTest asciidoctor`
- Files (advisory): 컨트롤러 다수(65개 메서드), `app/rule/api/ControllerLayerRules.java`, `GlobalResponse.java`
- Depends: 4
- Size: M (변경 파일 다수이나 기계적 치환)
- Status: [x] done

### Task 6: user 도메인 적용
- Acceptance: user 4개 컨트롤러(인증·팔로우·기본 정보·마이페이지)가 태그·summary·응답 스키마를 갖고 allowlist에서 제거된다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): user 문서 어노테이션, `AuthV2Controller`, `FollowController`, `UserBasicController`, `UserMyPageController`
- Depends: 4
- Size: M (5파일)
- Status: [x] done

### Task 7: support 도메인 적용 — 차단, 신고
- Acceptance: `BlockController`, `ReportCommandController`가 태그·summary·응답 스키마를 갖고 allowlist에서 제거된다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): block/report 문서 어노테이션, 컨트롤러 2개
- Depends: 4
- Size: S (4파일)
- Status: [x] done

### Task 8: support 도메인 적용 — 비즈니스 지원, 문의
- Acceptance: `BusinessSupportController`, `HelpCommandController`가 태그·summary·응답 스키마를 갖고 allowlist에서 제거된다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): business/help 문서 어노테이션, 컨트롤러 2개
- Depends: 4
- Size: S (4파일)
- Status: [x] done

### Checkpoint: after Tasks 6-8
- [x] `unit_test`, `integration_test`, `check_rule_test` 통과
- [x] allowlist 잔여 항목이 예상과 일치 (9건)

### Task 9: curation, rating 도메인 적용
- Acceptance: curation 2개와 `RatingController`가 태그·summary·응답 스키마를 갖고 allowlist에서 제거된다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): curation·rating 문서 어노테이션, `ProductCurationSpecController`, `ProductSpecBasedCurationController`, `RatingController`
- Depends: 4
- Size: M (5파일)
- Status: [x] done

### Task 10: picks, like, history 도메인 적용
- Acceptance: `PicksCommandController`, `LikesCommandController`, `UserHistoryController`가 태그·summary·응답 스키마를 갖고 allowlist에서 제거된다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): picks·like·history 문서 어노테이션, 컨트롤러 3개
- Depends: 4
- Size: M (6파일)
- Status: [x] done

### Checkpoint: after Tasks 9-10
- [x] `integration_test` 통과
- [x] 남은 대상이 banner, 파일 업로드, external 2개뿐임을 확인

### Task 11: banner, 파일 업로드, external 도메인 적용
- Acceptance: `BannerQueryController`, `ImageUploadController`, `AppInfoController`, `NotificationController`가 태그·summary·응답 스키마를 갖고 allowlist에서 제거된다
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"` 통과
- Files (advisory): banner·file·external 문서 어노테이션, 컨트롤러 4개
- Depends: 4
- Size: M (7파일)
- Status: [x] done

### Task 12: allowlist 소진 확인과 RestDocs 삭제 예정 표시
- Acceptance:
  - allowlist가 비고, 예외 없이 전수 검증이 통과한다 (성공 기준 1-4 자동 증명)
  - RestDocs·Antora 자산에 삭제 예정 표시가 존재하고 그 위치가 이 문서에 기록된다
  - 기존 `restDocsTest`, `asciidoctor`가 여전히 통과한다 (가정 3)
- Verification: `./gradlew :bottlenote-product-api:integration_test --tests "*OpenApiSpec*"`, `./gradlew restDocsTest asciidoctor`
- Files (advisory): allowlist 제거, 삭제 예정 표시 위치(build.gradle 주석 또는 docs 패키지 안내 문서), 이 plan 문서
- Depends: 5, 6, 7, 8, 9, 10, 11
- Size: S (4파일)
- Status: [x] done

## Progress Log

- 2026-07-26 define 작성. main 기준으로 `restdocs-api-spec` 파일럿 발견, springdoc 전면 전환으로 방향 확정.
- 2026-07-26 Task 9·10·11·12 완료. 큐레이션 2개·별점 3건, 찜하기·좋아요·활동 기록 4건, 배너·이미지 업로드·서버 정보 3건을 문서화해 **allowlist를 비웠다**. `Map`을 반환하는 서버 정보 엔드포인트는 실제 담기는 키를 문서용 record로 표현했다. 전수 검증이 예외 없이 통과해 성공 기준 1-4가 자동 증명됐다 — 실측 결과 fallback 태그 0건, summary 누락 0건, 빈 응답 스키마 0건, 한국어 태그 25종.
  Task 12의 삭제 예정 표시는 네 곳에 남겼다: ① `bottlenote-product-api/src/test/java/app/docs/DEPRECATED.md`(대체 수단과 함께 삭제될 자산 목록) ② `bottlenote-product-api/build.gradle`의 `asciidoctor` 태스크 주석 ③ 루트 `build.gradle`의 `restDocsTest` 태스크 주석 ④ `product-api.adoc` 상단 주석. 최종 검증으로 `integration_test`(OpenAPI), `check_rule_test`, `unit_test`, `restDocsTest`, `asciidoctor`, `build` 전부 통과했다(성공 기준 7·8 충족).
- 2026-07-26 Task 6·7·8 완료 및 Checkpoint(6-8) 통과. user 4개(인증·팔로우·회원 정보·마이페이지) 17건, support 차단 8건·신고 2건, 비즈니스 문의 5건·문의 5건을 문서화했다. 두 가지 실수를 잡았다 — ① allowlist 마지막 항목을 지울 때 닫는 괄호까지 삭제해 문법이 깨졌고 spotless가 즉시 잡아줬다 ② `GetAppleNonce`에 스키마 없는 빈 `@Content`를 지정해 springdoc이 추론한 `NonceResponse`를 덮어써, 반환 타입 기반 예외 처리가 무력화됐다. 스키마를 명시해 해결했다. 후자는 "감싸지 않는 응답"도 스키마를 명시해야 한다는 교훈이다. allowlist 잔여 9건.
- 2026-07-26 Task 5A 완료. 컨트롤러 65개 메서드의 반환 타입을 `ResponseEntity<GlobalResponse>`로 구체화하고(컴파일 통과, import 보강 불필요), `ControllerLayerRules`에 와일드카드 반환을 금지하는 ArchUnit 규칙을 추가했다. 규칙을 처음 `noMethods().should(...)`로 썼을 때 판정이 반전되어 위반을 잡지 못했고, `methods().should(...)` positive 형태로 고쳐 해결했다. 규칙이 공허하지 않음을 확인하려고 `RatingController` 한 곳을 일부러 와일드카드로 되돌려 실패를 관찰한 뒤 복구했다. 이 과정에서 `check_rule_test`가 Gradle `UP-TO-DATE`로 스킵되는 것을 발견했다 — 루트 `test` 블록의 `outputs.upToDateWhen { false }`가 커스텀 Test 태스크에는 없기 때문이다. 규칙 검증 시 `--rerun-tasks`가 필요하다. `integration_test`, `restDocsTest`, `asciidoctor` 모두 통과.
- 2026-07-26 가정 붕괴 발견과 해결 (Task 6 진행 중 정지 → 방향 수정 후 재개). `AuthV2Controller`의 3개 엔드포인트가 공통 형식 없이 DTO를 그대로 반환하는 것을 발견해 stop-condition 1번으로 정지했다. 사용자 제안에 따라 예외 목록 대신 **반환 타입 선언**을 구분 근거로 삼는 방식을 택했다. `GlobalResponse.ok()` 반환 타입을 좁히고(호출부 77곳 무영향 확인), Apple·카카오 로그인 2건에 실제 본문 타입을 선언했다. customizer는 `ResolvableType`으로 반환 타입을 읽어 감싸기를 건너뛴다. 검증 결과 3건은 DTO를 직접 가리키고 나머지는 공통 형식을 유지한다. 이 발견으로 `OpenApiDocsIntegrationTest`의 "모든 200 응답이 공통 형식"이라는 단정이 거짓이 되어, 공통 형식 대상과 예외 3건을 각각 검사하도록 고쳤다(검사가 약해진 것이 아니라 예외가 고정되어 오히려 강해졌다). Task 2에서 `NonceResponse` `$ref` 연결을 성공 증거로 보고했던 것은 오판이었고 정정했다.
- 2026-07-26 Task 5 완료 및 Checkpoint(4-5) 통과. alcohols 5개 컨트롤러(조회·탐색·인기·기준 정보·테이스팅 태그) 13개 엔드포인트를 문서화했다. 진행 중 `AlcoholPopularQueryController`가 서비스의 `List<PopularItem>`을 컨트롤러에서 `PopularsOfWeekResponse`로 감싸는 것을 발견해, 서비스 반환 타입이 아니라 컨트롤러가 실제로 내보내는 타입을 기준으로 문서를 고쳤다. 4개 엔드포인트 중 봄 추천만 배열, 나머지 3개는 `{totalCount, alcohols}` 형태다. 큐레이션 조회 2건은 `CursorResponse<T>`가 그대로 data에 담기므로 문서용 record로 표현했다. allowlist 잔여 17건.
- 2026-07-26 Task 4 완료. 배치 위치를 도메인별 분산(`{domain}.controller.docs.*ApiDocs`)으로 확정하고 review 3개 컨트롤러 12개 엔드포인트에 적용했다. 확정된 패턴은 위 "확정된 문서화 패턴" 절에 적었다. 탐색 엔드포인트는 `GlobalResponse.ok(Pair, params)` 경유로 실제 `data`가 `CollectionResponse<T>`임을 코드에서 확인해(추측하지 않고) 문서용 record로 표현했다. 생성 결과를 눈으로 확인해 태그 3종(리뷰·리뷰 댓글·리뷰 탐색), 요약 12건, `data` 실타입 연결 12건을 확인했다. allowlist 잔여 22건.
- 2026-07-26 Task 3 완료 및 Checkpoint(1-3) 통과. `OpenApiSpecQualityTest`가 태그·요약·요약과 메서드명 중복·빈 응답 스키마 네 가지를 전수 검사하고, 미문서화 컨트롤러 25개는 `PENDING_TAGS`로 예외 처리한다. 목록에 이미 문서화된 태그가 남으면 실패하는 검사도 넣어 목록이 낡지 않게 했다. 검증기가 공허하지 않음을 확인하려고 `review-controller`를 목록에서 임시로 빼 보았고 검사 3건이 예상대로 실패했다(복구 완료). 스펙 조회 로직은 `OpenApiSpecTestSupport`로 분리했다. `check_rule_test`와 기존 `restDocsTest`·`asciidoctor`도 통과해 가정 3(기존 체계 공존)을 확인했다.
- 2026-07-26 Task 2 보완. 실태 조사에서 `PUT /api/v1/picks`와 `GET /api/v1/banners`가 `ResponseEntity<GlobalResponse>`를 반환해 공통 형식이 `data` 안에 중첩되는 결함을 발견했다. customizer가 `GlobalResponse` 참조를 힌트로 취급하지 않도록 고쳤고 중첩 0건을 확인했다. 같은 조사에서 `GET /api/v2/auth/apple/nonce`가 `NonceResponse`를 `$ref`로 가리키는 것을 확인해 Task 2 Acceptance 2번도 증명됐다.
- 2026-07-26 실태 수치 정정. 스펙 실측 결과 paths 69개, operations 77개, 태그 25개다. define에 적은 71 paths는 이전 브랜치 기준이었다. 태그가 25개인 이유는 `NotificationController`가 본문 없는 빈 클래스여서 노출 엔드포인트가 없기 때문이다 — 문서화 대상이 아니므로 Task 11에서 제외한다. 성공 기준의 숫자를 실측값으로 바꿨다(가정 자체는 유지).
- 2026-07-26 Task 2 완료. `GlobalResponseSchemaCustomizer`(`OperationCustomizer`)로 2xx 응답을 공통 형식으로 감싼다. 컨트롤러가 `ResponseEntity<?>`를 반환해 반환 타입을 알 수 없으므로, 엔드포인트가 표준 `@ApiResponse`로 `data` 타입만 알려주면 customizer가 그것을 `data` 자리로 옮기고 success·code·errors·meta를 덧붙이는 구조로 만들었다. 이미 감싼 응답을 다시 감싸지 않도록 방어했고, 타입 정보가 없는 빈 object는 힌트로 취급하지 않는다. 통합 테스트 4건 통과 — 스펙의 모든 200 응답이 공통 5개 필드를 갖는다.
- 2026-07-26 Task 1 완료. springdoc 2.8.17을 카탈로그와 product-api에 추가하고, `springdoc.api-docs.path`를 `/openapi.product.json`으로 지정했다. swagger-ui는 설정으로 껐다. `SecurityPolicy` fallback이 `REQUIRED_AUTH`이므로 스펙 경로를 PUBLIC으로 명시했다. `OpenApiConfig`에 한국어 제목·설명과 액세스 토큰 인증 스키마를 정의했다. 통합 테스트 3건(무인증 200·OpenAPI 3.1 형식·swagger-ui 차단) 통과.
