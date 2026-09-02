# Workspace #429 Product MFDS 공개 조회

## Execution Mode
- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- stop-conditions: 가정 붕괴, 3회 verify 실패, 범위 밖 행동

## 계약
- Product 알코올 상세 응답에 검증 완료된 MFDS 수입신고 연계 정보를 read-only로 추가한다.
- 공개 조건: selectedAlcoholId 일치 AND normalizationStatus=NORMALIZED. selectedAlcoholId만 있고 REVIEW_REQUIRED 등 상태 강등이면 제외한다.
- 공개 필드명과 중첩 패턴은 현재 Admin MFDS 상세 계약과 일치시킨다.
- 공개 payload 타입은 mfds.facade.payload에 두고 facade 공개 계약 패턴을 따른다.
- 매칭 점수·매칭 사유·검토 메모·미해석 원문·운영 처리 상태 등 내부 정보는 제외한다.
- mfdsDeclarations는 항상 non-null 배열(없으면 빈 배열), importer는 nullable이다.
- 복수 신고의 수입사 조회는 importer ID 일괄 조회로 N+1을 피한다(최대 2회).
- 연결 없음 또는 검토중은 공개하지 않고 기존 상세 응답 호환성을 유지한다.
- 수집 job, 정제 규칙, 운영 승인, Admin 변경, DB migration은 범위 밖이다.

## 검증
- 테스트 소스를 먼저 추가하고 최소 구현한다.
- 로컬 Gradle/JVM/Testcontainers는 실행하지 않는다. 정적 diff 검증 후 GitHub Actions에서 compile/rule 및 integration을 확인한다.
- Product OpenAPI/JSON exact allowlist 계약 테스트를 갱신한다.
