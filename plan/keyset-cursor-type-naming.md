# ADR: Product 키셋 커서 타입 명명 정리 (#407)

## Status

- Accepted
- 기준: workspace #407, `main` `8cda193f`

## Context

`app.bottlenote.global.pagination`의 `Pagination`, `PaginationRequest`, `PageResponse`는 Product HMAC 키셋 커서 전용이지만 이름만 보면 offset/page-number 타입과 구분되지 않는다. 이번 변경의 목적은 Java 타입 이름을 명확히 하는 것이며 API 동작을 바꾸는 것이 아니다.

현재 공개 계약은 요청 `cursor`/`size`, 응답 `content`와 `pagination.hasNext`/`pagination.nextCursor`다. 타입 이름 변경 과정에서 record component, 생성 기본값, HMAC cursor, seek/order, JSON 직렬화가 달라지면 안 된다.

## Decision

다음 타입만 같은 패키지 안에서 이름을 바꾼다.

- `Pagination` → `KeysetPagination`
- `Pagination.PageSlice` → `KeysetPagination.PageSlice`
- `PaginationRequest` → `KeysetPageRequest`
- `PageResponse<T>` → `KeysetPageResponse<T>`

모든 import, 반환 타입, fixture, 단위 테스트는 새 이름으로 일괄 정리한다.

`CursorProperties`, `PaginationConfiguration`, `PaginationException`, `PaginationExceptionCode`, `HmacCursorCodec`, `CursorClaims`, `CursorKeys`는 현재 역할이 이름에 드러나므로 이번 범위에서 바꾸지 않는다.

## Compatibility invariants

- 요청 query key와 기본값은 동일하다: `cursor`, `size`.
- 응답 JSON key와 값은 동일하다: `content`, `pagination.hasNext`, `pagination.nextCursor`.
- `fromOverflow`의 size+1 slice, `hasNext`, 마지막 item cursor 생성은 동일하다.
- cursor encoding/verification, context, expiry, HMAC, seek/order는 수정하지 않는다.
- Product endpoint route, status, error envelope, OpenAPI 설명은 수정하지 않는다.
- Admin page/size 타입은 수정하지 않는다.

## Non-goals

- 패키지 이동
- Lombok 전환
- compact constructor/factory 정규화 변경
- 새 pagination abstraction 또는 adapter 추가
- API response field 변경
- cursor 알고리즘·성능 변경
- migration, dependency, configuration 변경

## Test strategy

1. 기존 pagination 단위 테스트를 새 타입명으로 이전한다.
2. ObjectMapper 기반 회귀 단위 테스트로 대표 `KeysetPageResponse` JSON이 기존 문자열과 정확히 같은지 확인한다.
3. 기존 요청 정규화와 overflow slice 단위 테스트를 유지한다.
4. compile → rule test → unit test만 검증한다. 개발 API 호출, integration test, Testcontainers, 배포 검증은 하지 않는다.
5. diff에서 rename 외 production 로직 변경이 없는지 확인한다.

## Execution Mode

- mode: delegated
- scope: implement, test, verify, commit, push, pr
- stop-conditions: 응답 JSON/기본값/cursor 로직 변화, 예상 밖 패키지·Admin 영향, compile/rule/unit 3회 내 미해결

## Progress Log

- 2026-08-22: live workspace #407과 `main` 코드를 확인하고 ADR을 확정했다.
