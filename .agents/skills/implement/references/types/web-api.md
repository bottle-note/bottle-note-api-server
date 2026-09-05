# BottleNote HTTP API

Product Java와 Admin Kotlin의 HTTP 계약을 변경할 때 읽는다. 프로젝트 지침의 `스킬 실행 원칙`과 [Java·Kotlin 패턴](../languages/java-spring.md)을 따른다.

## 구현 경계

Product Controller는 `bottlenote-product-api/src/main/java/app/bottlenote/`, Admin Controller·Docs는 `bottlenote-admin-api/src/main/kotlin/app/bottlenote/{domain}/presentation/`와 `presentation/docs/`에서 찾는다. 기존 경로·버전·메서드·상태 코드를 먼저 확인하고 주변 패턴을 유지한다.

Controller는 인증 주체와 요청 DTO를 해석하여 인터페이스를 호출한다. 도메인 VO 생성, 트랜잭션, 타 도메인 Facade 조합은 Service가 담당한다. 현재 보안 정책 어노테이션·필터를 확인하고 사용자별 소유권 검증을 보존한다. 선택 인증과 필수 인증을 혼동하지 않는다.

응답은 실제 `GlobalResponse`의 success·code·data·errors·meta 계약과 전역 예외 처리를 따른다. 다른 프레임워크의 error JSON이나 새 상태 코드 규칙을 끼워 넣지 않는다. Entity를 응답 DTO에 직접 노출하지 않는다. 공개 필드·타입·인증·정렬 의미가 바뀌면 호환성 영향과 사용자 결정을 확인한다.

## 페이징

현재 공통 구현은 mono의 `app/bottlenote/global/pagination/`에 있다. `KeysetPageRequest`, `KeysetPageResponse`, `KeysetPagination.fromOverflow`, `HmacCursorCodec`와 대상 Repository를 함께 읽는다. Product·Admin 모두 endpoint마다 계약이 다를 수 있으므로 “Admin은 항상 offset”이라고 가정하지 않는다.

- `pageSize + 1`개로 다음 페이지 여부를 판단하고 반환할 항목은 pageSize까지만 남긴다.
- nextCursor는 **반환하는 마지막 item**에서 만든다. 버리는 초과 item을 쓰면 다음 페이지 첫 항목이 누락될 수 있다.
- 현재 `KeysetPagination`은 다음 페이지가 없으면 nextCursor를 null로 반환한다.
- 정렬 값이 같을 때의 ID tie-breaker, ASC/DESC와 seek 조건, null 정렬, 검색 조건·사용자 범위에 묶인 cursor 검증을 보존한다.
- 빈 결과, 정확히 pageSize개, pageSize+1개, 연속 두 페이지의 누락·중복, 잘못되거나 다른 조건의 cursor를 테스트한다.

Offset API는 현재 endpoint의 page·size·total 계약을 유지하되 Spring Data 타입을 도메인 포트에 새로 노출하지 않는다. 새 코드는 실제 공통 타입과 해당 endpoint의 계약을 확인하여 작성한다.

## OpenAPI와 오류

Product는 `/api/v1/openapi.product.json`, Admin은 `/admin/api/v1/openapi.admin.json`을 제공한다. Docs 인터페이스·Controller의 어노테이션과 실제 GlobalResponse data 스키마, 오류 응답, 인증 정책을 함께 갱신한다.

품질 테스트는 실제 application context의 springdoc JSON을 읽어야 한다. 경로·operation·파라미터·명시적 응답 타입·bearer 요구사항과 공개 문서 정책을 확인한다. 상세 위치는 [테스트 참조](../../../test/references/testing/java.md)에 있다. 입력 실패와 비즈니스 예외가 구분되는지, 민감한 내부 정보가 오류나 로그에 노출되지 않는지도 확인한다.

## 부수효과와 다중 인스턴스

트랜잭션 안에서 publishEvent를 호출하고, 커밋 후 실행할 부수효과는 AFTER_COMMIT listener로 연결한다. 발행 호출을 “post-commit” 위치로 옮기지 않는다. 비동기 처리와 새 트랜잭션은 각각 `@Async`와 `REQUIRES_NEW`의 책임이다.

중복 요청 억제·카운트·분산 잠금은 공유 저장소의 원자성을 사용한다. JVM 로컬 값으로 인스턴스 전체의 동작을 보장한다고 가정하지 않는다. 목록 쿼리의 N+1·무제한 조회와 동일 요청의 중복 부수효과를 변경 범위에서 점검한다.
