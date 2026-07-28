```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-07-28

** Core Achievements **
- 시음회 requestSpec·responseSpec에 placeName·zipCode 추가 (둘 다 optional)
- placeName을 x-feed(order 25, role location)로 노출, zipCode는 피드 제외
- 프로그램 placeName·address를 address-search 계약으로 전환하고 zipCode 추가
- 전 단계 PASS: unit 502건 / rule 63건 / integration 268건 / admin integration 209건, 실패 0

** Key Components **
- resources/openapi/curation/whisky_tasting_event.json
- resources/openapi/curation/program.json
- curation/service/CurationPlaceFieldContractTest.java (계약·회귀 9건)

** Deferred Items **
- placeName·zipCode의 required 승격: FE 장소검색 연결 후
- CurationPayloadValidator의 pattern 지원: 이번은 리소스 JSON만 수정하는 범위
- 미정의 property 차단: FE가 보내는 다른 필드까지 깨질 수 있어 별건
- FE 연계(barAddress 전용 자동 동기화 제거, 필드별 매핑): FE 작업
================================================================================
```

# Plan: 큐레이션 시음회·프로그램 장소검색 계약 정렬

## Overview

이슈 bottle-note/workspace#344 구현. Admin 시음회 form이 `requestSpec`에 없는
`placeName`을 FE 하드코딩으로 주입해 저장하고 있다. 스펙을 SSoT로 되돌리고,
장소검색 계약(`x-field-style: address-search`)을 두 스펙에 정렬한다.

현재 상태를 실측으로 확인했다.

- 시음회 `requestSpec`에 `placeName`이 없는데도 개발 DB 14건 중 12건에 저장돼
  있다. `CurationPayloadValidator`가 미정의 property를 차단하지 않기 때문이다.
- 상세 API는 원본 payload를 그대로 내보내므로 `placeName`이 이미 응답에 나온다
  (`GET /v2/curations/6` → `placeName: "도시술"`). 반면 피드 API와 `feed_payload`
  에는 없다 — `responseSpec`에 없어 `x-feed` 투영에서 빠진다.
- `PROGRAM`은 `placeName`이 `requestSpec`·`responseSpec`에 모두 있고 `x-feed`도
  켜져 있다. 즉 두 스펙의 계약이 이미 어긋나 있으며, 시음회를 맞추는 것이
  일관성 회복이다.
- `zipCode`는 두 스펙 어디에도 없고 저장된 payload 15건 전부에 없다.

### Assumptions

- 신규 PR은 **PR #680 브랜치(`Whale0928/feed-read-path`) 위에 쌓는다.** #680이
  먼저 머지된 뒤 리뷰 가능하다. 스펙 변경이 #680의 재생성과 함께 동작하는 것을
  한 PR에서 보일 수 있다.
- 변경 대상은 스펙 JSON 리소스 두 개(`whisky_tasting_event.json`,
  `program.json`)다. 추출·투영·검증 로직은 건드리지 않는다.
- 스키마 변경 없음. Flyway 마이그레이션 없음.
- `placeName`과 `zipCode`는 **둘 다 optional**이다. required로 올리면
  `placeName` 없는 시음회 2건과 `zipCode` 없는 전 15건이 어드민 재저장 시
  `CURATION_PAYLOAD_INVALID`로 실패한다. FE가 장소검색을 연결해 데이터가 채워진
  뒤 required 승격은 후속 과제로 남긴다.
- 우편번호 필드명은 프로젝트 관례를 따라 `zipCode`다
  (선례: `review/facade/payload/LocationInfo`, DB `zip_code varchar(5)`).
  `postalCode`·`zonecode`는 프로젝트에 쓰이지 않는다.
- **[수정됨] `CurationPayloadValidator`는 `pattern`을 검증하지 않는다.** 지원하는
  제약은 type/required/nullable/enum/maxLength/minLength/maximum/minimum/
  maxItems/minItems뿐이며, 기존 큐레이션 스펙에 `pattern` 사용처도 없었다.
  따라서 `zipCode`에 `minLength: 5`, `maxLength: 5`를 함께 걸어 길이는 서버가
  강제하고, `pattern: ^\d{5}$`는 숫자 제약을 FE·문서용으로 남긴다.
  review 도메인은 Bean Validation `@Pattern`을 쓰지만 큐레이션 payload는
  자유형 JSON이라 같은 방식을 쓸 수 없다. 검증기에 `pattern` 지원을 추가하는
  것은 이번 범위(리소스 JSON만 수정) 밖이라 후속 과제로 남긴다.
- `barAddress`("장소 및 바(bar) 주소")와 `detailAddress`("상세 주소")는 이름·의미·
  스타일 모두 현행 유지한다. 이슈 표의 "선택 주소"가 `barAddress`, "상세 위치"가
  `detailAddress`에 대응한다.
- `placeName`은 `responseSpec`에도 추가하고 `x-feed`를 켠다. `PROGRAM`이 이미
  그렇게 되어 있어 두 스펙이 같은 모양이 된다.
- `zipCode`는 `responseSpec`에 넣되 `x-feed`는 켜지 않는다. 계약에는 드러내
  상세에서 보이게 하고, 피드 카드에는 노출하지 않는다.
- `PROGRAM`은 `address`와 `placeName` **둘 다** `address-search`로 바꾼다.
  이슈는 `address`만 언급하지만, 한쪽만 바꾸면 시음회와 계약이 어긋난다.
- 검증기의 미정의 property 허용 동작은 이번에 바꾸지 않는다. 막으면 FE가 보내는
  다른 필드까지 깨질 수 있어 별건이다.
- 배포 시 `responseSpec`이 바뀌므로 #680의 재생성이 두 스펙에 대해 자동 실행된다.
  이는 의도된 동작이며 별도 backfill 작업이 필요 없다.

### Success Criteria

- 시음회 `requestSpec`에 `placeName`(string, maxLength 100, `장소명`,
  `address-search`)과 `zipCode`(string, length 5 고정, `우편번호`,
  `address-search`)가 optional로 존재한다. `required` 배열은 기존 8개에서
  늘지 않는다.
- `zipCode`에 5자가 아닌 값을 넣으면 검증이 거부한다.
- `PROGRAM` `requestSpec`에 `zipCode`가 optional로 추가되고, `address`와
  `placeName`의 `x-field-style`이 `address-search`다.
- 시음회 `responseSpec`에 `placeName`이 `x-feed.enabled=true`로, `zipCode`가
  `x-feed` 없이 존재한다.
- 시음회 피드 응답 payload에 `placeName`이 포함된다 (현재는 빠져 있다).
- 시음회 피드 응답 payload에 `zipCode`가 포함되지 않는다.
- `placeName`이 없는 기존 시음회 2건도 생성·수정·조회가 모두 정상 동작한다.
- Admin curation-spec 상세 API 응답의 `requestSpec`에 변경이 반영된다.
- 기존 시음회·프로그램 payload가 검증을 통과한다 (회귀 없음).
- Product 상세 응답이 변경 전과 동일하다.

### Impact Scope

- **mono**: `resources/openapi/curation/whisky_tasting_event.json`,
  `resources/openapi/curation/program.json` — 리소스만 변경.
- **product-api / admin-api / test-support**: 코드 변경 없음. 스펙 리소스 변경에
  따른 테스트 기대값 조정만 있을 수 있다.
- **스키마**: 변경 없음.
- **배포**: `responseSpec` 변경으로 #680의 재생성이 시음회·프로그램 스펙에 대해
  자동 실행된다. 개발 DB 기준 시음회 14건 + 프로그램 1건의 `feed_payload`가 갱신된다.
- **API 계약**: 시음회 피드 응답에 `placeName` 필드가 **추가**된다. 가산적
  변경이라 기존 클라이언트를 깨지 않는다.

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- base 브랜치는 `Whale0928/feed-read-path`(PR #680)다. PR도 그 브랜치를 대상으로 연다.
- 구현·리뷰에 codex CLI를 활용해 작성자와 리뷰어를 분리한다.
- stop-conditions: 기본 3종 (① 가정 붕괴 시 재개봉 프로토콜, ② /verify 3회
  실패 시 /debug 보고 후 정지, ③ scope 밖 되돌리기 어려운 행동 전 확인)

## Tasks

### Task 1: 시음회 장소 필드 추가
- Acceptance: `requestSpec`에 `placeName`(string, maxLength 100, `장소명`,
  `address-search`)과 `zipCode`(string, length 5 고정 + `pattern ^\d{5}$`,
  `우편번호`, `address-search`)가 optional로 존재하고 `required` 배열은 기존
  8개 그대로다. `zipCode`가 5자가 아니면 검증이 거부한다.
  `responseSpec`에 `placeName`이 `x-feed.enabled=true`로, `zipCode`가 `x-feed`
  없이 존재한다. 그 결과 피드 응답 payload에 `placeName`이 포함되고 `zipCode`는
  포함되지 않으며, `placeName`이 없는 payload도 검증을 통과한다.
- Verification: `./gradlew :bottlenote-mono:test --tests '*Curation*'`,
  `./gradlew :bottlenote-product-api:integration_test --tests '*ProductSpecBasedCuration*'`
- Files (advisory): `openapi/curation/whisky_tasting_event.json`, 관련 테스트
- Depends: 없음
- Size: S
- Status: [x] done

### Task 2: 프로그램 장소검색 계약 정렬
- Acceptance: `requestSpec`에 `zipCode`가 optional로 추가되고, `address`와
  `placeName`의 `x-field-style`이 `address-search`다. 기존 PROGRAM payload가
  검증을 통과하고 피드·상세 응답 내용은 변하지 않는다(스타일은 렌더링 힌트일 뿐
  응답 값에 영향이 없다).
- Verification: `./gradlew :bottlenote-mono:test --tests '*Curation*'`
- Files (advisory): `openapi/curation/program.json`, 관련 테스트
- Depends: 없음 (Task 1과 병렬 가능 — 서로 다른 리소스다)
- Size: S
- Status: [x] done

## Progress Log

- /plan 완료: Task 2개(S 2), 서로 독립이라 병렬 가능. 리소스 JSON 하나씩이
  자연 관절이며, 회귀 확인은 각 Task의 Acceptance에 포함해 별도 Task로 쪼개지
  않았다. Task가 2개라 Checkpoint는 생략하고 마감의 `/verify full`이 그 역할을 한다.
  기존 테스트 8개 파일이 두 스펙을 참조하지만 payload 키 목록을 전수 단정하는
  곳은 없어 파급은 제한적으로 예상된다.
- 가정 붕괴/수정 1건: `CurationPayloadValidator`가 `pattern`을 검증하지 않음을
  구현 중 발견해 정지·보고했다. 사용자 결정으로 `minLength`/`maxLength` 5를 함께
  걸어 길이는 서버가 강제하고 `pattern`은 FE·문서용으로 남긴다. 검증기 확장은
  후속 과제.
- Task 1·2 완료: 시음회에 `placeName`(x-feed order 25, role location)·`zipCode`,
  프로그램에 `zipCode` 추가 및 `placeName`·`address`를 `address-search`로 전환.
  리소스 JSON은 재직렬화가 원본과 바이트 일치함을 확인한 뒤 편집해 포맷을 보존했다.
  `CurationPlaceFieldContractTest` 9건 신규.
  codex 리뷰 3건 중 2건 반영: ① `responseSpec`에 `placeName`이 들어가면서 상세
  경로(`materialize`)의 검증 대상이 된 점을 테스트가 증명하지 못함 → 기존 저장값
  통과와 100자 초과 거부를 각각 고정. 개발 DB 실측으로 기존 15건 최대 9자,
  비문자열 0건, 100자 초과 0건이라 회귀 없음을 확인했다. ② `order`·`role`
  미고정 → 시간(20)과 주소(30) 사이라는 관계로 고정. ③ `pattern` 미검증은
  이미 문서화된 결정이라 유지.
