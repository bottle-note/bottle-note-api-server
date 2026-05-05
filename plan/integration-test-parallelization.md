# Plan: 통합 테스트 병렬화 (DB-per-fork)

## Overview

윈도우 환경에서 `./gradlew integration_test` 실행 시간이 macOS(약 5분) 대비 약 2배 (현재 baseline **9분 51초**) 걸리는 문제 해결.

근본 원인은 단일 JVM 순차 실행 + 윈도우 Docker Desktop의 컨테이너 부팅 오버헤드. Testcontainers reuse 활성화는 이미 적용했으나, 테스트 자체의 wall-clock은 줄지 않음.

해결 전략: **공유 MySQL 컨테이너 1개 + fork별 독립 DB(스키마)** 패턴으로 데이터 격리를 유지하면서 Gradle `maxParallelForks`로 클래스 단위 병렬 실행.

### Assumptions

1. **파일럿 범위는 product-api 모듈의 `integration_test` 태스크만 적용** (확정). `admin_integration_test`는 파일럿 검증 후 별도 작업으로 분리.
2. **시작 fork 수는 2** (확정). 로컬 윈도우 8GB / GitHub Actions ubuntu-latest(2 vCPU / 7GB RAM) 모두 안전한 값. 안정화 확인 후 환경별 최적 fork 수 별도 튜닝.
3. MySQL 컨테이너는 1개만 유지. fork마다 같은 컨테이너에 접속하되 DB(스키마) 이름만 다르게 사용 (`bottlenote_product_w{workerId}`).
4. Liquibase는 Spring Boot 자동 설정에 의존. DataSource가 fork별 DB를 가리키면 Liquibase가 해당 DB에 마이그레이션을 자동 적용한다.
5. Redis는 동일 인스턴스의 **DB index 분리(0–15)**로 격리. fork id를 그대로 index로 사용.
6. MinIO는 fork별 **bucket 이름 격리** (`test-bucket-w{workerId}`).
7. **JUnit 5 클래스 내부 메서드 병렬은 활성화하지 않음** (확정). 클래스 단위 병렬(Gradle `maxParallelForks`)만 사용 — 트랜잭션/스레드 의존 테스트 보호.
8. MySQL 한정으로 `@ServiceConnection` 제거하고 수동 `DataSource` 빈으로 교체. Redis/MinIO의 `@ServiceConnection` 또는 기존 빈 구성은 유지하되 fork id만 주입.
9. Gradle이 각 test fork JVM에 `org.gradle.test.worker` 시스템 프로퍼티를 고유값(`1`, `2`, ...)으로 주입한다는 표준 동작에 의존.
10. 격리 누수로 인한 신규 flaky 테스트가 발생하면 즉시 롤백. 새 실패 0건이 전제.
11. **GitHub Actions 호환 필수** (확정). 로컬에서만 빨라지는 변경은 채택 불가. CI runner의 메모리/CPU 제약을 일급 제약으로 본다.
12. **Testcontainers reuse 미사용** (확정). 컨테이너 설정 변경 시 reuse 해시가 달라져 고아 컨테이너가 남는 문제, 그리고 Ryuk가 reuse 컨테이너를 청소하지 않는 구조적 한계 때문. CI에선 reuse 효과 없으므로 로스 0, 로컬에선 매 실행 컨테이너 부팅 비용을 감수하는 대신 환경 청결성 확보. 속도 향상은 병렬화에서 얻는다.

### Success Criteria

1. **로컬 윈도우 환경**: `./gradlew integration_test` wall-clock 시간이 baseline 9m 51s 대비 **40% 이상 단축** (목표 5분대).
2. **GitHub Actions 환경**: 기존 CI 워크플로의 통합 테스트 step 시간이 변경 전 대비 **단축 또는 동등** (악화 0). reuse가 cold-start CI에선 효과 없으므로 병렬화 자체로 단축 기대.
3. 통과한 통합 테스트 수가 baseline과 동일. **신규 실패 0건** (로컬·CI 모두).
4. 매 실행마다 fork DB 생성과 Liquibase 적용이 멱등하게 동작. 두 번째 실행에서도 신규 실패 없이 동일하게 통과.
5. fork 2개 환경에서 각각 독립 DB(`bottlenote_product_w1`, `bottlenote_product_w2`)에 Liquibase changelog가 적용 완료 — 두 DB의 `DATABASECHANGELOG` 테이블 동일.
6. **반복 실행 안정성**: 동일 통합 테스트 스위트를 연속 5회 실행해도 모두 통과 (flaky 0건).
7. **메모리 안전성**: 로컬 Docker 8GB 한도 내, GitHub Actions ubuntu-latest 7GB RAM 한도 내 OOM 미발생.

### Impact Scope

#### 변경 대상 파일
| 파일 | 변경 내용 |
|------|-----------|
| `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/TestContainersConfig.java` | MySQL `@ServiceConnection` 분리, fork DB 동적 생성, DataSource 빈 신규, MinIO bucket 명 동적화, Redis DB index 동적화 |
| `bottlenote-product-api/src/test/resources/application-test.yml` | Redis 설정에 worker id 기반 DB index 주입 가능하게 수정 (필요 시) |
| `build.gradle` (root, `subprojects` 블록) | `integration_test` 태스크에 `maxParallelForks`, `forkEvery`, JVM 메모리 설정 추가 |

#### 영향 받는 테스트
- `@Tag("integration")` 36개 클래스, 48개 사용처 (코드 변경 없이 격리 환경에서 실행 검증만)
- `MinioContainerLoadingTest` 등 bucket 이름 직접 의존하는 테스트 점검 필요

#### 영향 모듈
- `bottlenote-mono` — 테스트 인프라 (소스 변경)
- `bottlenote-product-api` — 통합 테스트 실행 환경 (설정 변경)
- `bottlenote-admin-api` — **이번 파일럿 범위 외** (admin_integration_test는 그대로 둠)
- `bottlenote-batch` — 영향 없음

#### 영향 받지 않는 것
- 프로덕션 코드 (도메인/서비스/컨트롤러)
- Liquibase changelog 파일 (서브모듈 `git.environment-variables`)
- 단위 테스트 (`unit_test`), 아키텍처 테스트 (`check_rule_test`), RestDocs 테스트

#### 신규 리스크
- fork 간 비동기 이벤트 누수 (예: Redis pub/sub) — 현재 사용 여부 확인 필요
- Quartz 스케줄러가 fork별로 시작 시 잡 중복 가능성 — 통합 테스트에서 Quartz 활성화 여부 점검 필요
- DataInitializer.deleteAll()의 동적 테이블 발견 로직이 fork 자기 DB만 보는지 검증 필요
- **GitHub Actions 메모리 압박**: ubuntu-latest는 7GB RAM. 2 fork × Spring context(~600MB) + MySQL(~500MB) + Redis + MinIO + Gradle daemon → 약 3GB로 안전 마진 있음. 단 JVM `-Xmx`를 fork당 1GB로 제한 필요.

#### CI(GitHub Actions) 환경 고려사항
- **Testcontainers reuse는 CI에서 효과 없음** — 매 워크플로 실행은 새 runner라 컨테이너 캐시 없음. 단축 효과는 순수 병렬화에서 나옴.
- **runner 스펙**: ubuntu-latest = 2 vCPU / 7GB RAM / 14GB SSD. 2 fork가 vCPU와 정확히 매칭됨.
- **Docker 가용성**: ubuntu-latest는 Docker 사전 설치됨, 별도 setup 불필요.
- **Liquibase 마이그레이션 비용 가중**: CI에선 매번 fresh DB → 2 fork × 마이그레이션 1회씩 = 약 +5–10초. 병렬화로 상쇄됨.
- **CI 워크플로 변경 필요 여부**: `maxParallelForks`가 Gradle 태스크 내부 설정이라 워크플로 YAML 수정 없을 가능성 높음. 단, GitHub Actions 캐시 키에 영향 없는지 확인.

## Tasks

### Task 1: MySQL DB-per-fork DataSource 구성 [x]
- 수용 기준:
  - `TestContainersConfig.mysqlContainer()`에서 `@ServiceConnection` 제거
  - 공유 컨테이너 부트스트랩 DB(`bottlenote_bootstrap`)는 모든 fork 동일 → reuse 해시 일치, 컨테이너 1개만 띄워짐
  - `org.gradle.test.worker` 시스템 프로퍼티 기반으로 fork별 DB(`bottlenote_product_w{id}`) 동적 생성 (`CREATE DATABASE IF NOT EXISTS`, 멱등)
  - 수동 `DataSource` 빈이 fork별 DB로 JDBC URL 라우팅
  - Liquibase가 fork DB에 changelog 자동 적용 (Spring Boot 자동 구성 그대로)
- 검증:
  - `./gradlew :bottlenote-product-api:integration_test` (단일 fork 상태에서) 통과
  - 컨테이너 진입해 `SHOW DATABASES`로 `bottlenote_product_w0` 존재 확인
  - `DATABASECHANGELOG` 테이블이 fork DB에 존재
- 파일:
  - `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/TestContainersConfig.java`
- 크기: S (1 file)
- 상태: [ ] 미완료

### Task 2: Redis DB index 격리
- 수용 기준:
  - Redis 컨테이너는 그대로 1개 공유
  - fork별 `spring.data.redis.database = {workerId}`로 분리 (0–15 범위 내, fork 수 ≤ 16 보장)
  - 두 fork가 동시에 Redis에 키 쓰더라도 서로 보이지 않음 (수동 검증)
- 검증:
  - 단위 검증용 임시 통합 테스트로 두 fork에서 동일 키에 다른 값 쓰고 cross-read 없는지 확인 (또는 기존 캐시 의존 통테 통과로 갈음)
  - 기존 Redis 사용 통테 (`@Tag("integration")` 중 캐시 의존)가 단일 fork에서 그대로 통과
- 파일:
  - `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/TestContainersConfig.java`
  - `bottlenote-product-api/src/test/resources/application-test.yml` (필요 시 placeholder 추가)
- 크기: S (1–2 files)
- 상태: [ ] 미완료

### Task 3: MinIO bucket fork별 격리
- 수용 기준:
  - 컨테이너는 1개 공유, bucket 이름만 `test-bucket-w{workerId}`로 분리
  - `TestContainersConfig.getTestBucket()`이 worker id를 반영한 이름 반환
  - bucket 이름을 직접 참조하는 테스트 (`MinioContainerLoadingTest`, `ImageUploadIntegrationTest` 등) 점검 후 정적 상수 의존 제거
- 검증:
  - `./gradlew :bottlenote-product-api:integration_test --tests "*Image*"` 단일 fork 통과
  - MinIO console 또는 `aws s3 ls` 동등 명령으로 두 bucket 존재 확인
- 파일:
  - `bottlenote-mono/src/test/java/app/bottlenote/operation/utils/TestContainersConfig.java`
  - `bottlenote-product-api/src/test/java/app/bottlenote/common/file/upload/MinioContainerLoadingTest.java` (참조 점검)
  - `bottlenote-product-api/src/test/java/app/bottlenote/common/file/integration/ImageUploadIntegrationTest.java` (참조 점검)
- 크기: S (최대 3 files)
- 상태: [ ] 미완료

### Checkpoint: Task 1–3 완료 후 (격리 인프라)
- [ ] 컴파일 통과 (`./gradlew compileTestJava`)
- [ ] 단일 fork에서 `integration_test` 100% 통과 (격리 적용 후 baseline 동등성 확인)
- [ ] 아키텍처 규칙 통과 (`./gradlew check_rule_test`)
- [ ] 컨테이너 reuse 동작 확인 (두 번째 실행 시 `Reusing container` 로그)

### Task 4: Gradle integration_test 병렬 fork 활성화
- 수용 기준:
  - `build.gradle`의 `tasks.register('integration_test', Test)` 블록에 다음 추가:
    - `maxParallelForks = 2`
    - `forkEvery = 0` (JVM 재사용)
    - `jvmArgs '-Xmx1g', '-XX:MaxMetaspaceSize=256m'` (CI 7GB 마진 확보)
  - admin_integration_test 태스크는 변경하지 않음 (파일럿 범위 외)
  - JUnit Jupiter 메서드 병렬 설정은 추가하지 않음
- 검증:
  - `./gradlew :bottlenote-product-api:integration_test` 실행 중 `Test worker 1`, `Test worker 2` 로그 동시 발생 확인
  - 통합 테스트 100% 통과
- 파일:
  - `build.gradle` (root, subprojects 블록 내 통합 테스트 태스크)
- 크기: S (1 file)
- 상태: [ ] 미완료

### Task 5: 로컬 환경 성능·안정성 검증
- 수용 기준:
  - 통합 테스트 5회 연속 실행, **신규 실패 0건** (flaky 검증)
  - wall-clock 시간 baseline 9m 51s 대비 40% 이상 단축 (≤ 5m 54s)
  - Docker 메모리 사용량 8GB 한도 내 유지 (모니터링 1회)
  - 두 번째 실행에서 컨테이너 reuse 동작, Liquibase 마이그레이션 스킵 로그 확인
- 검증:
  - `for /l %i in (1,1,5) do @gradlew :bottlenote-product-api:integration_test` (Windows) 또는 bash 동등 스크립트로 5회 실행
  - 시간 측정 데이터 plan 문서 Progress Log에 기록
- 파일:
  - 없음 (검증 작업)
- 크기: S
- 상태: [ ] 미완료

### Task 6: GitHub Actions 환경 검증
- 수용 기준:
  - PR 또는 dry-run 워크플로로 CI에서 `integration_test` 실행
  - 기존 CI 시간 대비 단축 또는 동등, 악화 0
  - OOM 또는 신규 실패 없음
  - 워크플로 YAML 변경 없이 통과 (변경 필요 시 별도 후속 Task로 분리)
- 검증:
  - GitHub Actions UI에서 step 시간 비교 (변경 전 main 브랜치 vs 변경 후 PR)
  - workflow run 로그에서 fork worker 동시 실행 확인
- 파일:
  - 없음 (CI 검증). 단, 메모리 문제 발생 시 `.github/workflows/*.yml` 점검 필요할 수 있음
- 크기: S
- 상태: [ ] 미완료

### Checkpoint: Task 4–6 완료 후 (성공 기준 충족)
- [ ] 로컬 wall-clock ≤ 5m 54s (40% 단축)
- [ ] 5회 연속 실행 신규 실패 0건
- [ ] CI 시간 악화 0
- [ ] OOM 미발생 (로컬·CI)
- [ ] plan 문서에 측정 데이터 기록 완료

## Progress Log

### 2026-05-05 — Baseline 측정
- Reuse 활성화 + Docker 6코어/8GB 적용 후
- `./gradlew integration_test` 결과: **9분 51초** (BUILD SUCCESSFUL)
- 기존 윈도우 환경 15–20분 대비 약 절반 단축

### 2026-05-05 — Task 1 완료
- 변경 파일:
  - `bottlenote-mono/.../TestContainersConfig.java`: `@ServiceConnection` 제거, 부트스트랩 DB 패턴, `@Primary DataSource` 빈 추가, **MySQL/Redis `withReuse(true)` 제거**
  - `bottlenote-product-api/.../application-test.yml`: `spring.datasource` 블록 제거
- 검증 결과:
  - 컴파일 통과
  - 컨테이너 내부 확인: `bottlenote_bootstrap` + `bottlenote_product_w{n}` fork DB 동적 생성, Liquibase 133 changeset 적용
- 정책 변경: **Testcontainers reuse 미사용**. 고아 컨테이너 누수 방지. CI 영향 없음, 로컬은 매 실행 컨테이너 부팅 비용 부담.
- 측정 (단일 fork, Task 4 적용 전):
  - reuse 활성 baseline: 9m 51s
  - Task 1 적용 + reuse 활성: 8m 31s
  - **Task 1 적용 + reuse 제거 + 클린 슬레이트: 8m 30s**
  - 36개 통합 테스트 클래스 모두 통과, 신규 실패 0건
- 잔여 컨테이너 정리: testcontainers 라벨 컨테이너 일괄 삭제 완료
