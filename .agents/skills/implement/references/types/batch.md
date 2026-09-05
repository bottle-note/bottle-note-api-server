# BottleNote Batch

Spring Batch·Quartz 작업을 바꿀 때 읽는다. 프로젝트 지침의 `스킬 실행 원칙`이 우선하며, 배포나 운영 Job 실행은 별도 명시 허가가 필요하다.

## 실제 구조

- 구현은 `bottlenote-batch/src/main/java/app/batch/bottlenote/`에 있다. `job/popularity/`의 JobConfig·Tasklet과 해당 Quartz 연결을 함께 확인한다.
- 테스트는 `bottlenote-batch/src/test/java/app/batch/bottlenote/`에 있다. `job/popularity/` 테스트와 `BatchApplicationContextTest`를 참고한다.
- 의존성·태그·SQL 패키징은 `bottlenote-batch/build.gradle`을 기준으로 삼는다. 공용 API 테스트 헬퍼의 사용 방식을 Batch에 자동으로 복제하지 않는다.

Scheduler는 파라미터·스케줄·Job 실행을 연결하고, Job은 Step 순서와 성공·실패 전이를 정의한다. Tasklet·Reader/Processor/Writer는 대상 작업 방식에 맞춰 기존 구현을 따른다. 사용자 동작 하나를 레이어별 대규모 변경으로 넓히지 않는다.

## 데이터와 실행 계약

대상 기간·업무 시간대·버킷 경계·정렬·중복 키를 명시한다. 실행 시각과 처리 대상 날짜를 혼동하지 않으며, 집계의 입력 구간은 실제 쿼리·테스트에서 확인한다. 같은 기간 재실행, 중간 실패 후 재개, 일부 결과만 저장된 경우의 동작을 정의한다.

출력이 성공하기 전에 체크포인트·완료 표시를 앞당기지 않는다. 후속 snapshot·정리 작업은 선행 작업의 성공 조건을 유지한다. 예를 들어 popularity rollup·snapshot·retention의 순서를 바꿀 때는 실패 시 다음 Step이 실행되는지까지 검사한다.

다중 인스턴스의 중복 실행 방지는 공유 DB·Redis·Quartz 설정을 확인한다. JVM static이나 로컬 파일은 전체 인스턴스의 소유권 근거가 아니다. 기존 JobParameters와 재시작 정책을 확인하지 않고 매번 임의 UUID를 넣어 중복 실행을 허용하지 않는다.

병렬화는 실제 DB pool·worker 수·트랜잭션 범위를 기준으로 제한한다. 입력 전체 적재, 무제한 재시도, 항목별 client 생성은 피한다. 일시 장애에만 제한된 재시도를 적용하고 영구 실패나 일부 실패를 성공으로 바꾸지 않는다. 성능 수치는 측정한 경우에만 보고한다.

## 테스트와 검증

Batch는 `batch_test`에서 `@Tag("batch")` 테스트를 실행한다. 공통 `unit_test`, `integration_test`, `check_rule_test`, `admin_integration_test`는 Batch 모듈에서 비활성화되어 있다.

현재 CI의 `unit-tests` job은 `app.batch.bottlenote.job.popularity.*`만 선택한다. 이 범위에도 `PopularityRollupTaskletMySqlIntegrationTest`의 MySQL Testcontainers 검사가 포함되어 Docker가 필요하다. Docker가 필요 없다는 기존 workflow 주석보다 실제 테스트 구성을 따른다. `deploy_batch.yml`의 `prepare-build`는 `BatchApplicationContextTest` 등을 포함한 전체 Batch 검사를 수행한다. 테스트가 없다고 가정하거나 부분 검사를 전체 Batch 검증으로 보고하지 않는다.

핵심 규칙·집계 경계는 Fake/인메모리 DB 등 기존 테스트 방식으로, 실제 SQL·컨텍스트·리소스 계약은 필요한 통합 테스트로 검증한다. 테스트 태그만으로 Docker 필요 여부를 단정하지 않는다. 명시적 로컬 검사와 Actions 관찰 명령은 [검증 참조](../../../verify/references/verify/java-gradle.md)에 있다.

## 리소스와 배포 경계

Batch는 Flyway를 실행하지 않는다. 현재 main·test resources에는 서브모듈에서 지정한 `storage/mysql/sql/popularity.sql`, `storage/mysql/sql/best-review-selected.sql`만 복사한다. 환경 설정·마이그레이션 전체를 포함하지 않는다. `verifyBatchPackagedResources`는 누락·금지 리소스를 검사하는 별도 태스크이므로 실제 실행 여부를 확인한다.

이미 존재하는 배포 workflow 결과는 정확한 소스 SHA로 연결하여 읽을 수 있다. CI 증거를 만들기 위한 workflow dispatch·rerun이나 로컬 이미지 빌드·push·GitOps 수정은 수행하지 않는다. 애플리케이션 Ready와 실제 Job 성공은 별도 증거다.
