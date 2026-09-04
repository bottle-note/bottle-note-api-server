# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

> If a `CLAUDE.personal.md` file exists in the same directory, also refer to its contents.
> **Team instructions take priority over personal instructions when they conflict.**

## User Instructions

- Always respond in Korean regardless of question language.
- If the user asks in English and there are grammar errors, provide grammar feedback in the response.
- For complex or ambiguous questions, include a summary of your understanding in the response.
- Keep answers clear and concise (3-5 lines recommended).
- When code examples are needed, ask whether to show all at once or step by step.
- When writing code, keep comments to a single brief line.
- When modifying existing code, always follow the project's existing patterns and conventions.
- When multiple files need changes, list the files to be modified upfront.
- For error resolution, present a step-by-step approach.
- When multiple solutions exist, recommend the one most fitting the project context first.

## Project Overview

- **Stack**: Spring Boot 3.4.11, Java 21, MySQL, Redis, QueryDSL
- **Architecture**: DDD-based multi-module structure
- **Core Domains**: alcohols, user, review, rating, support, history, picks, like

## 모듈 구조

> [진행 중] 모듈 기술 부채 정리 작업 진행 중 (2026-06-10 시작). 모듈 구성은 아래가 확정 토폴로지이며(물리 3분할은 트리거 조건부로 강등), 남은 작업은 mono 내부 표준 위반 청소와 ArchUnit 룰 활성화다.
>
> 현재 모듈: `product-api`(Java) / `admin-api`(Kotlin) / `batch` / `mono`(공유 라이브러리) / `observability` / `test-support`(테스트 인프라·픽스처). test-support 분리는 PR #623으로 완료(2026-06-12), 각 모듈은 testImplementation으로 소비한다.
> 레이어 규칙은 아래 "레이어 표준 15"를 따른다.

```mermaid
graph LR
    P["product-api (Java, bootJar)"] --> M["mono (공유 라이브러리)"]
    A["admin-api (Kotlin, bootJar)"] --> M
    B["batch (Java, bootJar)"] --> M
    M --> O[observability]
    TS["test-support (테스트 인프라)"] -- api --> M
    P -. testImplementation .-> TS
    A -. testImplementation .-> TS
    M -. testImplementation .-> TS
```

## 빌드 및 실행

```bash
# 서브모듈 초기화 (최초 클론 후 필수)
git submodule update --init --recursive

./gradlew build                 # 전체 빌드
./gradlew test                  # 기본 테스트 (integration, data-jpa-test 제외)
./gradlew unit_test             # 단위 테스트 (@Tag("unit"))
./gradlew integration_test      # 통합 테스트 (@Tag("integration"))
./gradlew check_rule_test       # 아키텍처 규칙 테스트 (@Tag("rule"))
./gradlew bootRun               # 애플리케이션 실행

# admin-api 모듈 전용
./gradlew :bottlenote-admin-api:build           # admin-api 빌드
./gradlew :bottlenote-admin-api:test            # admin-api 테스트 실행
./gradlew :bottlenote-admin-api:bootRun         # admin-api 실행
```

### 서브모듈

- **git.environment-variables**: 환경 설정과 DB 마이그레이션 SQL을 담은 비공개 서브모듈
  - `storage/db/migration/*.sql`: Flyway 마이그레이션 원본
  - `storage/mysql/sql/*.sql`: batch 전용 쿼리 리소스
  - 빌드와 통합 테스트 실행 전 서브모듈 초기화 필수

## 배포 및 실행 인프라

- 운영·개발 모두 Kubernetes에서 돌아가고 GitOps로 배포한다. k8s 매니페스트는 이 저장소가 아니라 별도 저장소에 있다.
- **애플리케이션 인스턴스는 다중이다.** 요청 카운팅, 분산 잠금, 중복 억제, 스케줄 단일 실행 같은 것을 JVM 로컬 상태(static 필드, 인메모리 캐시)로 구현하면 안 된다. Redis 등 공유 저장소를 쓴다.
- 앞단 게이트웨이가 클라이언트를 통해 들어온 `X-Forwarded-For`를 제거하고 실제 접속 주소로 다시 채운다. 따라서 앱이 받는 XFF는 신뢰할 수 있다.

### 배포 방법

배포는 모두 GitHub Actions로 수행한다. **로컬에서 이미지 빌드·푸시·매니페스트 수정을 직접 하지 않는다.**

| 대상 | 워크플로 | 트리거 |
|------|---------|--------|
| product-api / admin-api (운영) | `release_pr_pilot_create.yml` → `release_pr_pilot_merged.yml` → `deploy_release_applications.yml` | Release PR 생성 후 병합 (표준 경로) |
| product-api / admin-api (운영, 레거시) | `deploy_release_applications.yml` | `backend/vX.Y.Z` 릴리스 published (호환 경로, 표준 아님) |
| product-api / admin-api (개발) | `deploy_development_applications.yml` | main CI 성공 시 자동, 또는 수동 dispatch |
| batch | `deploy_batch.yml` | 수동 dispatch (`environment`, `version` 입력) |

**`releases/**` 브랜치를 수동으로 만들지 않는다.** `release_pr_pilot_create.yml`만 만든다. 릴리즈 브랜치는 그 시점 소스의 `.github/workflows` 사본을 그대로 들고 오므로, 손으로 만들면 낡은 워크플로 스냅샷이 운영 경로에 다시 들어온다 (2026-08-31 admin-dashboard 사고 원인).

이미지 공개는 `immutable 태그 push → cosign 서명 → 검증 → 채널 태그 승격 → 재검증` 순서를 강제하며, 승격 이전 어느 단계가 실패해도 `*_latest_production`은 이전 digest를 유지한다. 상세 계약은 `k8s-platform`의 `docs/BottleNote 배포 운영 가이드.md` 5.3절이다.

```bash
# batch 배포 — version은 정확한 X.Y.Z, production은 main에서만 허용
gh workflow run deploy_batch.yml -f environment=production -f version=1.2.3

# 개발 환경 수동 배포
gh workflow run deploy_development_applications.yml
```

## 이슈 관리

이슈의 SSOT는 [bottle-note/workspace](https://github.com/bottle-note/workspace) 레포지토리다. 이슈는 이 레포가 아니라 workspace에 등록하고, 작업 시작 전 관련 이슈를 먼저 확인한다. PR 본문에는 `bottle-note/workspace#N` 형식으로 관련 이슈를 링크한다.

## Skills (Development Workflow)

Use these skills to follow the structured development lifecycle:

| Command | Purpose | When to Use |
|---------|---------|-------------|
| `/define` | Requirements clarification | Starting a new feature, vague requirements |
| `/plan` | Task breakdown | After /define, multi-file changes |
| `/implement` | Incremental implementation | Building features (product + admin) |
| `/test` | Test creation | Unit, integration, OpenAPI quality tests |
| `/verify` | Local CI verification | Compile, unit test, integration test |
| `/debug` | Systematic debugging | Build/test failures, unexpected errors |
| `/self-review` | Pre-commit quality gate | Before every commit |

**Lifecycle:** `/define` -> `/plan` -> `/implement` (with `/self-review` per Task) -> `/test` -> `/verify full`

**Detailed patterns** for product-api and admin-api implementation are in skill reference files:
- `.claude/skills/implement/references/languages/java-spring.md` — Java/Spring 구현 패턴
- `.claude/skills/implement/references/languages/bottlenote-patterns.md` — 프로젝트 특화 패턴 (InMemory 갱신 체크리스트 등)
- `.claude/skills/test/references/testing/java.md` — Java 테스트 패턴

## 에이전트 설정 미러 규칙

이 저장소는 codex와 Claude Code용 설정을 이중으로 보관한다. **한쪽을 고치면 반드시 다른 쪽도 같이 고친다.**

| 대상 | codex | Claude Code |
|---|---|---|
| 지침 | `AGENTS.md` | `CLAUDE.md` |
| 스킬 | `.agents` 디렉터리 | `.claude` 디렉터리 |

- 두 지침 문서는 런타임 이름, 스킬 디렉터리 경로, personal 지침 파일명, 첫 줄 제목만 다르고 나머지 본문은 완전히 같아야 한다. 이 네 가지 외의 차이는 동기화 누락이다.
- 두 스킬 디렉터리는 바이트 단위로 같아야 한다. `diff -rq`로 확인하고, 스킬 파일 안에 특정 런타임의 디렉터리 경로를 하드코딩하지 않는다.
- Skills 표에 적은 슬래시 커맨드는 실제 스킬 디렉터리로 존재해야 한다. 스킬을 제거하면 표에서도 지운다.
- 훅을 다시 도입한다면 머신 고유 절대경로와 런타임 전용 환경변수를 넣지 않는다. 저장소 루트는 `$(git rev-parse --show-toplevel)`로 구한다.

## 코드 작성 규칙

### 아키텍처 패턴

- **계층 구조**: Controller → Facade <-> Service → Repository → Domain
- **도메인별 패키지**: constant, controller, domain, dto, repository, service, facade, exception, event

**설계 배경 — Facade는 도메인 간 완충 지대다.** 여러 도메인이 양방향으로 통신하는 구조(user↔alcohols 등)에서 Service끼리 직접 상호 참조하면 순환 의존과 부채가 쌓인다. 그래서 타 도메인 접근은 상대의 Facade 인터페이스로만 하며, 결합을 좁고 보이는 한 지점으로 모아 관리한다. 배치 상세는 아래 `@FacadeService` 항목, 강제 수단은 ArchUnit(`./gradlew check_rule_test`)이다.

### 레이어 표준 15 (2026-06-10 확정)

모듈 기술 부채 정리의 기준이 되는 표준. ArchUnit `@Tag("rule")` 테스트로 강제하며, 위반 0이 된 룰부터 활성화한다.

1. 도메인 모델 != `@Entity`가 원칙이나, 구현 중복 제거를 위해 도메인 엔티티(JPA 엔티티)를 허용한다. 의도된 트레이드오프다.
2. 레포지토리는 2형으로 분리한다: 순수 자바 인터페이스인 도메인 레포지토리(포트) + 구현 레포지토리(JPA, QueryDSL, Redis).
3. 도메인 레포지토리 시그니처에 Spring Data 타입(`Page`, `Pageable`, `Slice`, `Sort`) 노출 금지. 페이징은 자체 타입(`PageResponse`, cursor criteria)을 쓴다.
4. 기술 세부사항(QueryDSL, EntityManager, Redis)은 구현 레포지토리 안에 격리하고 포트 밖으로 새지 않는다.
5. 코드는 자기 도메인 패키지에만 둔다. 타 도메인 파일을 자기 패키지에 두지 않는다.
6. 타 도메인 접근은 Facade 경유만 허용한다. 타 도메인의 repository는 물론 service도 직접 참조하지 않는다.
7. Facade는 인터페이스 + 별도 구현체로 구성한다. Service가 Facade를 겸직하지 않는다.
8. Controller는 인터페이스에만 의존한다. 구현체 직접 주입 금지, 도메인 VO 직접 생성 금지.
9. 트랜잭션 경계, 도메인 VO 생성/검증, cross-domain 조합은 Service가 소유한다.
10. 테스트 더블은 Mockito가 아니라 포트와 Facade의 Fake/InMemory 구현을 쓴다.
11. 공통 타입(`PageResponse`, cursor criteria, 공통 예외 베이스, 공통 어노테이션)은 `global`(공유 커널)에 둔다.
12. global은 어떤 도메인도 모른다. global → 도메인 import 금지.
13. 동기 호출은 Facade, 부수효과 전파(히스토리, 알림 등)는 도메인 이벤트로 한다.
14. 예외는 도메인이 소유한다. 도메인별 예외는 자기 exception 패키지에, 공통 베이스만 global에 둔다.
15. 운영 코드는 테스트 코드를 모른다. 테스트 인프라(컨테이너 설정, persistent factory, fake)는 test-support 모듈이 소유한다.

### 네이밍 컨벤션

- **클래스**: `{도메인명}Controller`, `Default{도메인명}Facade`, `Jpa{도메인명}Repository`, `{도메인명}Exception`
- **메서드**: get/find/search (조회), create/register (생성), update/modify/change (수정), delete/remove (삭제)

### 프로젝트 특화 어노테이션

| 어노테이션 | 위치 | 포함 | 용도 |
|---|---|---|---|
| `@FacadeService` | 구현체 `Default{도메인명}Facade`는 `{domain}.service`, 인터페이스는 `{domain}.facade`(공개 계약) | `@Service` | 도메인 간 완충 계층 구현체. 타 도메인은 Facade 인터페이스만 호출한다 |
| `@DomainRepository` | `{domain}.domain` | 없음 (마커) | Spring/JPA 비의존 도메인 레포지토리 인터페이스 |
| `@JpaRepositoryImpl` | `{domain}.repository` | `@Repository` | 도메인 레포지토리의 JPA 구현체, 영속성 예외 변환 |
| `@DomainEventListener` | `{domain}.event` | `@Component` | 도메인 이벤트 리스너, `ProcessingType`으로 동기/비동기 지정 |
| `@ThirdPartyService` | `app.external` | `@Service` | AWS·외부 API 등 써드파티 연동 계층 |

> 코드 예시: `.claude/skills/implement/references/languages/java-spring.md`

### 예외 처리

- 도메인별 예외: `{도메인명}Exception`, `{도메인명}ExceptionCode`
- 전역 예외 핸들러: `@RestControllerAdvice`
- 통일된 응답: `GlobalResponse`

### 코드 스타일

- Lombok: `@Getter`, `@Builder`, `@RequiredArgsConstructor`
- 불변성: `record` 사용 (DTO), `final` 필드 선호
- 페이징: `PageResponse`, `CursorPageable`

## 테스트 작성 규칙

### 테스트 분류 및 네이밍

- `@Tag("unit")`: 단위 테스트, `@Tag("integration")`: 통합 테스트, `@Tag("rule")`: 아키텍처 규칙
- 클래스명: `{기능명}ServiceTest`, 메서드명: `{기능명}할_수_있다`
- `@DisplayName`: 한글로 테스트 목적 명시 (형식: `~할 때 ~한다`)

### 테스트 구조

- Given-When-Then 패턴 사용
- Fixture 클래스를 통한 테스트 데이터 관리
- TestContainers 사용 (실제 DB 환경)
- 테스트 데이터: test-support 모듈의 `{도메인명}TestFactory` / `DataInitializer`로 생성

### 단위 테스트 패턴

- **Fake/Stub 패턴 선호**: Mock 대신 InMemory 구현체 사용
- **네이밍**: `InMemory{도메인명}Repository`, `Fake{서비스명}`
- **위치**: `bottlenote-test-support` 모듈의 `{도메인}.fixture` 패키지 (특정 모듈 전용 헬퍼는 그 모듈 test에 둔다)

### 통합 테스트 패턴

- **베이스 클래스**: `IntegrationTestSupport` 상속
- **API 테스트**: `MockMvcTester` 사용
- **테스트 데이터 생성**: `{도메인명}TestFactory` 사용

> Detailed code examples: see `/test` skill references (`test-infra.md`, `test-patterns.md`)

### 이벤트 기반 아키텍처

- **이벤트 발행**: `ApplicationEventPublisher.publishEvent()`
- **이벤트 수신**: `@TransactionalEventListener` + `@Async` 조합
- **트랜잭션 분리**: `@Transactional(propagation = Propagation.REQUIRES_NEW)`
- **이벤트 클래스**: `{도메인명}{동작}Event` record로 정의

## 데이터베이스 설계

### 스키마 마이그레이션 (Flyway)

스키마는 Flyway가 애플리케이션 기동 시점에 적용한다. `ddl-auto`는 `validate`이며 Hibernate가 스키마를 생성하거나 변경하지 않는다. 엔티티를 바꾸면 마이그레이션도 함께 작성해야 하고, 그러지 않으면 기동 시 validate에서 실패한다.

- 마이그레이션 원본은 `git.environment-variables/storage/db/migration/`에 두고, product-api와 admin-api의 `processResources`가 빌드 시 `classpath:db/migration`으로 복사한다. 저장소 소스 트리에는 `db/migration` 디렉터리가 없다.
- 설정은 `enabled: ${FLYWAY_ENABLED:true}`, `baseline-on-migrate: false`, `locations: classpath:db/migration`이다. 기존 스키마에 임의로 baseline을 잡지 않는다.
- batch 모듈은 `flyway.enabled=false`다. 마이그레이션 주체가 아니며, batch jar에 마이그레이션 SQL이 섞여 들어가면 빌드가 실패하는 가드가 있다. batch는 `storage/mysql/sql`의 지정된 쿼리 리소스만 패키징한다.
- 통합 테스트도 `flyway.enabled=true`로 동작한다. TestContainers 스키마 역시 Flyway가 만들며, 별도 init 스크립트를 쓰지 않는다.
- 새 마이그레이션은 서브모듈 저장소에 추가한 뒤 서브모듈 포인터를 갱신해야 반영된다.

### JPA 엔티티

- `BaseEntity` 상속 (공통 필드)
- 복합 키: `@Embeddable` 사용
- 엔티티 필터링: Hibernate `@Filter` 활용

### 레포지토리 계층 구조

레이어 표준 2·4가 원칙이다: 포트(순수 인터페이스)와 구현을 분리하고, 기술 세부사항은 구현 안에 격리한다.

1. **도메인 레포지토리** (필수): `{도메인명}Repository`, `{domain}.domain` 위치, `@DomainRepository`는 선택. Spring/JPA 비의존 순수 인터페이스이며 Service는 여기에만 의존한다.
2. **JPA 레포지토리** (필수): `Jpa{도메인명}Repository`, `{domain}.repository` 위치, `@JpaRepositoryImpl` 필수. `JpaRepository<T, ID>` 상속 + 도메인 레포지토리 구현. 단순 조회는 메서드 쿼리 또는 `@Query` JPQL로 해결한다.
3. **QueryDSL 레포지토리** (복잡한 쿼리만): `Custom{도메인명}Repository` / `Custom{도메인명}RepositoryImpl` / `{도메인명}QuerySupporter`(`@Component`), 전부 repository 패키지. 동적 조건 조합·다중 조인·복잡한 Projection에만 쓰고, 단순 CRUD나 단일 조건 조회에는 쓰지 않는다.

> 구현 예시: `.claude/skills/implement/references/languages/java-spring.md`

## 보안 및 인증

- JWT 토큰: 액세스 토큰 24시간, 리프레시 토큰 30일
- 토큰 검증: `JwtTokenProvider`, 보안 설정: `SecurityConfig`
- API 보안: `@PreAuthorize` 또는 `@Secured`, CORS: `WebConfig`

## 외부 서비스 연동

- OpenFeign: `@FeignClient`, 설정 분리 `FeignConfig`, 에러 처리 `ErrorDecoder`
- AWS S3: PreSigned URL 생성 (SDK v2 `S3Client`/`S3Presigner`)

## GSL Execution Mode

GSL 스킬(define/plan/implement/test/verify/debug/self-review)은 `plan/{기능}.md` 문서를 공유 상태로 쓰는 개발 생명주기다. 진행 방식은 `/define` 승인 게이트에서 계약으로 확정되며, **plan 문서의 `## Execution Mode` 섹션이 유일한 근거다.** 문서에 선언이 없으면 step-by-step이다.

### step-by-step (기본값)

- 각 GSL 스킬은 자기 작업과 종료 보고를 마치면 **턴을 끝낸다**. 다음 스킬 실행에는 사용자의 새 메시지가 필요하다.
- `/implement`는 Task 하나를 커밋하고 보고한 뒤 정지한다. 예외: 사용자가 여러 Task의 연속 실행을 명시적으로 지정한 경우("Task 1~3 진행해", "결과까지 진행해")에만 이어서 진행한다.
- 애매한 "계속", "continue"는 **다음 스킬로의 전환** 허가가 아니다 — 다음 명령을 안내하라는 뜻으로 해석한다. 단, 진행 중인 스킬 내부의 재개 신호는 그 스킬의 규정을 따른다 (`/implement`: 다음 Task 1개만 허가).
- 종료 보고에는 다음 권장 명령을 한 줄로 제안한다 (실행하지 않는다).
- 스킬 하나가 다른 스킬의 전체 워크플로를 내부에서 실행하지 않는다 (`/implement`가 Task 곁에 테스트 코드를 쓰는 것은 허용, `/test`·`/verify`·`/self-review`의 풀 워크플로 실행은 금지).

### delegated (define 승인 시 명시 선언)

`/define` 승인 게이트에서 아래 형식으로 선언하고 plan 문서에 기록한 경우에만 유효하다.

```markdown
## Execution Mode
- mode: delegated
- scope: plan, implement, test, verify, commit   # push, pr 포함 가능
- stop-conditions: 기본 3종 (+ 추가 조건)
```

- scope에 포함된 단계는 생명주기 순서(plan → implement → test → verify → commit → push → pr)대로 단계 간 승인 없이 이어 실행한다. 이는 위임 계약의 이행이므로 step-by-step의 스킬 격리 규칙이 적용되지 않는다. 단 Task마다 Progress Log 기록과 체크포인트 보고는 남긴다.
- `push`가 scope에 없으면 커밋까지만 하고 푸시 직전에 정지한다. `pr`이 있으면 PR 오픈과 본문 작성까지 수행한다 — **이 저장소는 PR 본문이 체인지로그를 겸한다.**

### stop-conditions (모드 무관, 무조건 정지)

1. **가정 붕괴** — 작업 중 발견이 define의 Assumption을 깨면 즉시 정지하고 재개봉 프로토콜(define 수정 → WHAT이 바뀌었으면 재승인)을 따른다. 조용히 적응하지 않는다.
2. **verify 반복 실패** — `/verify` 실패를 3회 시도 안에 해결하지 못하면 `/debug` 결과를 보고하고 정지한다.
3. **scope 밖 행동** — 선언된 scope 밖의 되돌리기 어려운 행동(푸시, PR 오픈, 파일 대량 삭제, 인프라 변경)이 필요해지면 그 직전에 정지하고 확인받는다. step-by-step에서 scope는 사용자가 명시적으로 승인한 작업 범위를 뜻한다.
