# BottleNote 검증 명령과 근거

프로젝트 지침의 `스킬 실행 원칙`과 [verify](../../SKILL.md)의 실행 방식이 우선한다. 아래 명령은 실행 권한을 추가하지 않는다. 기준 파일은 저장소 루트의 `build.gradle`, 각 모듈의 `build.gradle*`, `.github/workflows/ci_pipeline.yml`, `.github/workflows/deploy_batch.yml`이며 변경되면 그 내용을 먼저 반영한다.

## Actions 읽기 전용 조회

설치된 `gh run list --help`, `gh run view --help`, `gh pr checks --help`, `gh pr view --help`와 `gh api --help`로 지원 옵션을 확인한다. 아래 자리표시자는 실제 저장소·SHA·PR·run 값으로 바꾼다.

```bash
git status --short
git rev-parse HEAD
gh run list --workflow ci_pipeline.yml --commit <SHA> --limit 30 --json databaseId,attempt,headSha,event,status,conclusion,url
gh run view <RUN_ID> --attempt <ATTEMPT> --json headSha,event,attempt,status,conclusion,jobs,url
gh pr view <PR> --json headRefOid,baseRefOid,potentialMergeCommit,mergeCommit,statusCheckRollup,url
gh pr checks <PR> --required --json name,state,bucket,workflow,link
```

`gh pr checks`의 `bucket`은 요약 분류다. 원래 `state`, 각 Actions job의 `status`·`conclusion`을 함께 보며, 종료 코드 8은 pending이다. `gh run view --exit-status`의 종료 코드 0만으로 완료를 판정하지 않는다.

목록이 비었으면 PR checks의 링크와 workflow 트리거·조회 범위를 확인한다. 오래된 실행은 목록 제한 때문에 안 보일 수 있다. 필요하면 아래 GET 조회에서 pagination을 사용한다. 인증·네트워크 오류는 `missing`과 구분한다.

```bash
gh api --method GET 'repos/{owner}/{repo}/actions/runs?head_sha=<SHA>&per_page=100' --paginate --jq '.workflow_runs[] | {id,run_attempt,head_sha,event,status,conclusion,html_url}'
gh api --method GET 'repos/{owner}/{repo}/actions/runs/<RUN_ID>' --jq '{head_sha,event,run_attempt,path,pull_requests}'
gh api --method GET 'repos/{owner}/{repo}/git/commits/<TESTED_SHA>' --jq '{sha,parents:[.parents[].sha]}'
```

PR의 head 커밋과 임시 merge 커밋은 다르다. 현재 CI의 `prepare`는 PR head를 표시하지만 검증 job의 `actions/checkout`은 별도 `ref`를 지정하지 않는다. 따라서 표시된 head SHA만 보고 테스트 소스가 같다고 단정하지 않는다. 해당 run의 이벤트·당시 workflow·checkout 로그에서 실제 SHA를 확인하고, merge 커밋이면 parents와 PR head/base 관계를 기록한다. `potentialMergeCommit`은 현재 PR의 후보이며 과거 run의 증거가 아니다. 머지 후 생성된 실제 커밋도 이전 PR head·임시 merge SHA와 구분한다.

checkout 로그는 필요한 구간만 읽고 민감한 값을 마스킹한다. head 변경·base 갱신·rerun attempt가 다른 실행을 섞지 않는다. push CI는 정확한 커밋을, PR CI는 확인한 head와 base의 조합을 검증한 것으로 보고한다.

## 현재 CI의 검사 범위

`ci_pipeline.yml`은 main 대상 PR, main push, workflow_dispatch에 반응한다. 일반 feature branch push만으로 이 CI가 생긴다고 가정하지 않는다.

- `prepare`: 메타데이터를 수집한다.
- `unit-tests`: `./gradlew unit_test`와 `./gradlew :bottlenote-batch:batch_test --tests "app.batch.bottlenote.job.popularity.*"`를 실행한다.
- `rule-tests`: `./gradlew check_rule_test`를 실행한다.
- `integration-tests`: product(`integration_test`)와 admin(`admin_integration_test`) matrix를 실행한다. 표시 이름은 조회된 job 이름을 사용한다.
- `product-ci-final-build`: 위 테스트 jobs에 의존하며 `./gradlew build -x test --build-cache --parallel`을 실행한다. 이름과 달리 명령은 전체 빌드다.
- `test-report`: 실패 시에도 JUnit 리포트를 발행한다. 성공했다고 테스트가 모두 통과한 것은 아니다.

이 목록은 workflow의 검증 구성이다. 브랜치 보호·ruleset의 required checks와 같다고 가정하지 말고 PR의 required checks를 별도로 조회한다. 설정을 확인할 권한이 없으면 required 목록은 미확인으로 보고한다. 필요한 job 또는 테스트 step이 skipped·missing이면 CI 검증 완료로 처리하지 않는다.

Batch의 전체 `batch_test`는 `deploy_batch.yml`의 `prepare-build` job에서 실행한다. PR CI의 popularity 부분 통과는 전체 Batch 통과가 아니다. 배포 run이 이미 있으면 정확한 소스 SHA와 테스트 step 결과를 관찰하되, 검증만 하려고 이 workflow를 dispatch 또는 rerun하지 않는다.

## Gradle 태그와 결과

- 기본 `test`는 `integration`, `admin_integration` 태그를 제외한다. 단위 테스트만 고르는 명령이 아니며 `rule`·`batch`·다른 태그·무태그 테스트는 별도 설정이 없다면 포함될 수 있다.
- `unit_test`, `check_rule_test`, `integration_test`, `admin_integration_test`는 각각 `unit`, `rule`, `integration`, `admin_integration` 태그를 선택한다.
- Batch는 위 네 태스크를 비활성화하고 `batch_test`에서 `batch` 태그를 선택한다. `unit_test` 통과로 Batch까지 검증했다고 보고하지 않는다.
- 루트 설정은 Test 태스크의 up-to-date 및 build-cache 결과 재사용을 막는다. 그래도 task disabled, 필터 불일치, NO-SOURCE, skip은 실제 테스트 실행과 구분해야 한다.
- 수치는 해당 실행의 `<module>/build/test-results/<task>/TEST-*.xml` 또는 CI JUnit artifact에서 확인한다. 오래된 로컬 XML이나 중복 artifact를 합산하지 않는다.

## 명시적 로컬 검사

JDK 21과 필요한 의존성·서브모듈 리소스를 먼저 확인한다. standard의 Batch popularity 범위에도 MySQL Testcontainers 검사가 포함되므로 Docker가 필요하다. full의 통합·전체 Batch 검사도 Docker 환경을 준비한다. 누락된 환경을 이유로 하위 수준 통과를 요청 수준 통과로 바꾸지 않는다. 서브모듈의 인증정보·운영 설정을 출력하거나 기존 포인터를 임의로 변경하지 않는다.

루트 `compileJava`는 Spotless가 적용된 모듈에서 `spotlessApply`를 의존한다. 테스트와 build도 컴파일을 통해 소스를 포맷할 수 있다. 검증용 예제는 `-x spotlessApply`로 자동 수정을 제외하고 `spotlessCheck`로 포맷을 검사한다. 이것은 포맷 검사나 컴파일 자체의 비활성화가 아니다. QueryDSL 생성 코드, build 디렉터리, processResources 등 다른 부수효과는 남으므로 읽기 전용 진단에서 실행할 명령으로 간주하지 않는다.

각 수준은 필요한 태스크 집합이다. quick → standard → full을 모두 순서대로 실행하지 않으며, 이미 확인한 동일 소스·범위의 결과는 재사용한다.

- quick: Java 및 Admin Kotlin의 운영·테스트 컴파일, 포맷, 아키텍처 규칙을 확인한다.
- standard: 포맷·규칙·단위 테스트와 패키징을 확인하고 CI의 Batch popularity 범위를 포함한다.
- full: standard의 검사 범위에 Product·Admin 통합과 전체 Batch 검사를 포함한다. 전체 Batch가 popularity를 포함하므로 별도 중복 실행하지 않는다.

```bash
# local quick
./gradlew compileJava compileTestJava :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin spotlessCheck check_rule_test -x spotlessApply

# local standard
./gradlew build unit_test check_rule_test -x test -x spotlessApply --build-cache --parallel
./gradlew :bottlenote-batch:batch_test --tests 'app.batch.bottlenote.job.popularity.*' -x spotlessApply

# local full
./gradlew build unit_test check_rule_test integration_test admin_integration_test :bottlenote-batch:batch_test -x test -x spotlessApply --build-cache --parallel
```

구현 중 최소 검사는 실제 변경 대상과 소비 모듈을 선택한다. 예를 들어 아래 명령은 해당 Product 테스트만 검증하며 전체 프로젝트 통과 증거가 아니다.

```bash
./gradlew :bottlenote-product-api:unit_test --tests '<FULLY_QUALIFIED_TEST_CLASS>' -x spotlessApply
```

마지막으로 `git diff --check`, `git diff`, `git status --short`로 의도하지 않은 변경을 확인한다. 실패 로그는 필요한 원인만 마스킹하여 보고한다. 테스트 실패를 해결할 수정 권한은 기존 요청에서 판단하며, 검증 요청만으로 코드·설정·Git 상태를 바꾸지 않는다.
