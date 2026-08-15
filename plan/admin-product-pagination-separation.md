# Plan: Admin/Product 페이지네이션 계약 분리

## Overview

#402에서 Product 목록을 HMAC keyset cursor로 옮기며 Admin help·lookup이 같은 계약을 따라갔다. Admin과 Product의 페이지네이션 계약을 모듈 경계에서 분리한다.

- Issue: bottle-note/workspace#404
- 선행: bottle-note/workspace#402
- 계약: bottle-note/workspace#385
- 대상: Admin help 목록 원복, Admin 위스키 룩업 분리, Admin의 Product 목록 구현 직접 의존 제거, 불필요 cursor secret 제거
- 제외: Product 목록 HMAC cursor 20개 재구현, Admin 목록 전면 커서 전환, FE 전환(#394)

### Assumptions

1. #404만 구현한다. Product `GET /api/v1/help`를 포함한 Product 목록 20개의 HMAC cursor 계약은 유지한다.
2. Admin help "원복"은 예전 숫자 `cursor`+`pageSize` offset이 아니라, 다른 Admin 목록과 같은 `page`+`size`와 `GlobalResponse.fromPage` 응답(meta.page / size / totalElements / totalPages)이다.
3. Admin 위스키 룩업은 Product `AlcoholLookupService` / `AlcoholLookupRequest`를 직접 쓰지 않는다. 요청은 `page`+`size`이고, 응답도 Admin 전용이다.
4. Admin 컨트롤러는 Product 목록 Service/Repository(커서 코덱·`PageResponse` HMAC 타입을 끌어오는 경로)를 직접 쓰지 않는다. Product 페이징 타입만 바꿔도 Admin 목록 컨트롤러가 다시 깨지면 안 된다. 여기에는 Admin이 `AlcoholQueryService`를 통해 Product explore/cursor 빈에 묶이는 결합도 포함된다.
5. 룩업 분리 후 Admin이 `CursorProperties` / HMAC 코덱을 쓰지 않으면 `PAGINATION_CURSOR_SECRET`을 Admin yml(테스트 yml 포함)에서 제거한다. 빈 스캔 때문에 기동이 시크릿을 계속 요구하면 그 의존을 끊는다. 남기면 이유를 plan에 문서화한다.
6. DB 스키마·Flyway 변경은 없다.
7. 이미 `page`+`size`인 Admin 목록(위스키 검색, 리뷰, 유저, 배너, 큐레이션, 스펙 큐레이션, 지역, 증류소, 테이스팅 태그)은 바꾸지 않는다.
8. Access-control IP ban/signal의 `max` 상한 목록과 `GET /v2/curation-specs` 전량 참조 목록은 #404 변환 대상이 아니다. 감사 결과만 남긴다.
9. 테스트는 Fake/InMemory를 우선하고, Admin 통합 테스트로 help·lookup의 page/size 계약을 확인한다.

### Success Criteria

- Product 목록 API 20개는 HMAC cursor 계약(`cursor`+`size`, `meta.pagination`)을 유지한다.
- Admin `GET /helps`는 `page`+`size`로 동작하고 Product `GET /api/v1/help` 커서와 독립이다.
- Admin `GET /alcohols/lookup`은 Product lookup 서비스/요청을 직접 쓰지 않고 `page`+`size`로 동작한다.
- Product pagination 타입 변경만으로 Admin 목록 컨트롤러가 컴파일/기동에서 다시 깨지지 않는다.
- Admin 기동이 Product cursor secret에 의존하지 않는다. yml에서 제거되어 있다.
- Admin 목록 API 전수 감사 결과가 plan에 기록되어 있다. #404 범위 밖 예외(access-control `max`, curation-specs 전량)가 명시되어 있다.

### Impact Scope

- `bottlenote-admin-api`: help/lookup 컨트롤러·문서·yml, 기동 시 cursor secret 제거
- `bottlenote-mono`: Admin help 목록 계약, Admin 룩업 전용 경로, Admin이 Product 목록 구현을 직접 쓰지 않도록 분리
- `bottlenote-product-api`: Product help/lookup 계약 유지. 공유 시그니처가 바뀌면 테스트 더블만 맞춤
- Persistence: 스키마 변경 없음
- 외부 API: Admin help/lookup 요청·응답 키가 page/size로 바뀜. Product 공개 계약은 불변

### Admin 목록 감사 (조사 결과)

| 엔드포인트 | 현재 계약 | #404 조치 |
|---|---|---|
| `GET /helps` | HMAC `cursor`+`size` | page/size로 원복 |
| `GET /alcohols/lookup` | Product lookup 공유, HMAC cursor | Admin 전용 page/size로 분리 |
| `GET /alcohols` | page/size | 유지 |
| `GET /reviews` | page/size | 유지 |
| `GET /users` | page/size | 유지 |
| `GET /banners` | page/size | 유지 |
| `GET /curations` | page/size | 유지 |
| `GET /v2/curations` | page/size | 유지 |
| `GET /v2/curations/feed` | page/size | 유지 |
| `GET /regions` | page/size | 유지 |
| `GET /distilleries` | page/size | 유지 |
| `GET /tasting-tags` | page/size | 유지 |
| `GET /v2/curation-specs` | 전량 참조 목록 | 범위 밖 |
| `GET /access-control` | `max` 상한 | 범위 밖 |
| `GET /access-control/signals` | `max` 상한 | 범위 밖 |

## Execution Mode

- mode: delegated
- scope: plan, implement, test, commit, push, pr
- verify: local `/verify` 생략. PR 오픈 후 GitHub Actions로 트래킹한다.
- stop-conditions:
  1. 가정 붕괴 시 즉시 정지하고 재개봉
  2. GitHub Actions 실패를 3회 시도 안에 해결하지 못하면 `/debug` 보고 후 정지
  3. scope 밖 행동(머지, 인프라 변경, 파일 대량 삭제) 직전 정지

## Tasks

### Task 1: Admin help 목록을 page/size로 원복
- Acceptance: Admin `GET /helps`가 `page`+`size`와 `GlobalResponse.fromPage` meta(page/size/totalElements/totalPages)로 동작한다. Product `GET /api/v1/help` HMAC cursor는 유지된다.
- Verification: `./gradlew :bottlenote-mono:compileJava :bottlenote-admin-api:compileKotlin :bottlenote-product-api:compileJava -q`
- Files (advisory): `AdminHelpPageableRequest`, `HelpRepository`, `CustomHelpQueryRepository`, `CustomHelpQueryRepositoryImpl`, `AdminHelpService`, `AdminHelpController`, `AdminHelpApiDocs`, `InMemoryHelpRepository`
- Depends: 없음
- Size: M
- Status: [x] done

### Checkpoint: after Task 1
- [x] Admin help 요청 키가 page/size다
- [x] Product help 커서는 그대로다

### Task 2: Admin 위스키 룩업을 Product lookup에서 분리
- Acceptance: Admin `GET /alcohols/lookup`이 Product `AlcoholLookupService`/`AlcoholLookupRequest`를 직접 쓰지 않고 `page`+`size`로 동작한다. Product lookup HMAC cursor와 `data.items`는 유지된다.
- Verification: `./gradlew :bottlenote-mono:compileJava :bottlenote-admin-api:compileKotlin :bottlenote-product-api:compileJava -q`
- Files (advisory): Admin lookup request/service, `AdminAlcoholsController`, `AdminAlcoholsApiDocs`, lookup unit test
- Depends: 없음
- Size: M
- Status: [ ] not done

### Checkpoint: after Task 2
- [ ] Admin lookup이 Product lookup 요청/서비스를 직접 쓰지 않는다
- [ ] Product lookup 계약이 유지된다

### Task 3: Admin을 Product 목록 서비스에서 분리하고 cursor secret을 제거한다
- Acceptance: Admin 컨트롤러가 Product 목록 Service(`AlcoholQueryService`, `AlcoholLookupService`)를 주입하지 않는다. Admin yml/테스트 yml에서 `PAGINATION_CURSOR_SECRET`이 없고, Admin 기동이 cursor secret 없이 된다.
- Verification: `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-mono:compileJava -q` 및 Admin 기동 경로에서 cursor 설정이 없는지 확인
- Files (advisory): `AdminAlcoholQueryService`, `AdminAlcoholsController`, `AlcoholQueryService`, `PaginationConfiguration`, Admin `application.yml`, Admin `application-test.yml`
- Depends: Task 2
- Size: M
- Status: [ ] not done

## Progress Log

- 2026-08-16: /define. #404 WHAT 초안 작성. Admin 목록 전수 감사 포함.
- 2026-08-16: Execution Mode 확정. delegated, scope=plan/implement/test/commit/push/pr. local verify 생략, PR 후 GitHub Actions 트래킹.
- 2026-08-16: /plan. Task 3개로 분해. help 원복 → lookup 분리 → Product 서비스 분리+secret 제거.
- 2026-08-16: Task 1 완료. Admin help를 page/size + fromPage로 원복. Product help HMAC 커서는 유지. AdminHelpServiceTest 통과.
