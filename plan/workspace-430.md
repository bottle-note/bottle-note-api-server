# Workspace #430 알코올 별점 노출 정규화

## Execution Mode
- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- stop-conditions: 가정 붕괴, 3회 verify 실패, 범위 밖 행동

## 계약
- 기준은 리뷰 목록이 아니라 알코올 단위 별점 노출이다.
- 전체 별점은 해당 alcoholId 점수들의 평균이며 표시값은 소수점 첫째 자리로 정규화한다.
- 인증 사용자 별점은 userId + alcoholId로 조회되는 사용자 리뷰 1건의 점수를 노출한다.
- 4.0/4.5/4.6 및 둘째 자리 이상 집계값을 검증하고 0.5 단위 양자화는 하지 않는다.
- 기존 별점 필터, 원천 점수 저장, FE UI, DB migration은 범위 밖이다.

## 검증
- 테스트 소스를 먼저 추가하고 최소 구현한다.
- 로컬 Gradle/JVM/Testcontainers는 실행하지 않는다. 정적 diff 검증 후 GitHub Actions에서 compile/rule 및 integration을 확인한다.
- 목록·상세의 공통 노출 경로와 JSON 숫자 직렬화 계약을 확인한다.
