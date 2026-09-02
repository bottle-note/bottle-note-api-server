# Workspace #434 MFDS 원장 목록 SKU 표시명

## Execution Mode
- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- stop-conditions: 가정 붕괴, 3회 verify 실패, 범위 밖 행동

## 계약
- Admin `GET /admin/api/v1/mfds/declarations` 목록 항목에 nullable `skuDisplayNameKo`, `skuDisplayNameEn`을 추가한다.
- 필드명·패턴은 Admin 상세의 동명 필드와 일치시키고 mapper가 도메인 값을 그대로 전달한다.
- 기존 baseProductName, 검색/필터/페이징 계약은 유지한다.
- 신규 추출·정규화·백필·DB migration·FE 변경은 범위 밖이다.

## 검증
- 테스트 소스를 먼저 추가하고 최소 구현한다.
- 로컬 Gradle/JVM/Testcontainers는 실행하지 않는다. 정적 diff 검증 후 GitHub Actions에서 compile/rule 및 integration을 확인한다.
- mapper와 Admin 목록 OpenAPI/HTTP 계약을 검증한다.
- 후속 리뷰에서 nullable SKU 표시명의 mapper null 유지와 HTTP JSON 키 존재를 회귀 테스트로 고정한다.
