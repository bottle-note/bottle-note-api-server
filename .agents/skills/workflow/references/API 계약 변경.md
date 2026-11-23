# API 계약 변경

1. 기존 OpenAPI, 인증·검증 코드, 계약 테스트에서 현재 동작을 확인한다. [define](../../define/SKILL.md)의 작성 기준으로 합의된 변경과 관찰할 수용 기준을 적는다. 사용자 동작이나 호환성에 중요한 미결정 사항은 해당 변경을 진행하기 전에 확인한다.
2. [plan](../../plan/SKILL.md)의 기준으로 구현 순서와 영향 범위를 정한다. 기존 호출자, 권한, 오류 응답과 필요한 마이그레이션을 확인한다.
3. [implement](../../implement/SKILL.md)의 [Web API 패턴](../../implement/references/types/web-api.md)에 따라 구현한다. 관련 OpenAPI 정의를 갱신한다.
4. [test](../../test/SKILL.md)로 실제 Spring 애플리케이션의 OpenAPI JSON과 영향을 받는 HTTP 동작을 확인한다. 검사 범위는 변경된 계약에 맞춘다.
5. 호출자에게 달라지는 동작, 호환성 영향, 확인한 검증과 미검증 항목을 보고한다.
