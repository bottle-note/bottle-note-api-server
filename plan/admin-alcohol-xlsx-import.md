# Admin 알코올 XLSX 템플릿 및 업로드 검증

## Summary

Admin 사용자가 내부 엔티티 필드명을 알지 않아도 고정된 한글 템플릿으로 알코올 데이터를 작성하고, 등록 전에 서버 검증 결과를 확인할 수 있게 한다. 이번 범위는 템플릿 다운로드와 업로드 검증까지이며 실제 일괄 등록은 포함하지 않는다.

## Grounded Current State

- Admin 알코올은 현재 `POST /v1/alcohols`에서 단건 즉시 생성한다.
- 현재 `AdminAlcoholUpsertRequest.imageUrl`은 `@NotBlank`지만 `alcohols.image_url` 물리 컬럼은 nullable이고 생성 서비스는 null 이미지 이벤트를 무시한다.
- 지역·증류소·테이스팅 태그는 기존 참조 데이터이며 알코올 생성은 내부적으로 ID를 사용한다.
- Admin 모듈에는 XLSX 처리 의존성과 업로드 API가 없다.
- 작업 기준은 API-server live `main` d92ce1eb5661735887a8ec30f7c6f2db16f13931이며, 검증 환경의 private submodule은 live `main` b7fc28909625dc9fa3bf8db25a88d5cf9602baac를 checkout한다. 기능 PR에는 migration이 없으므로 submodule gitlink 변경을 포함하지 않는다.

## Execution Mode

- mode: delegated
- implementer: Grok via Hermes xAI provider
- scope: plan, implement, test, verify, commit
- excluded: push, PR, merge, deploy, release, workspace issue mutation
- stop-conditions: 가정 붕괴, 3회 내 verify 실패, scope 밖 행동, Grok provider 미인증

## Public Contract

### 1. 템플릿 다운로드

- `GET /v1/alcohols/excel/template`
- Admin bearer 인증 정책을 그대로 적용한다.
- 응답 content type은 OOXML XLSX이며 attachment filename을 제공한다.
- 주 입력 시트명은 `알코올 데이터`로 고정한다.
- 1행은 아래 고정 한글 필드명, 2행은 각 필드의 고정 한글 설명, 3행부터 데이터다.
- 보조 시트는 `지역`, `증류소`, `테이스팅 태그`, `입력 안내`이며 현재 참조 데이터를 표시하고 가능한 값에 데이터 유효성 드롭다운을 제공한다.
- 내부 Java/Kotlin 필드명이나 엔티티 필드명은 워크북에 노출하지 않는다.

고정 입력 열 순서:

1. 한글 이름
2. 영문 이름
3. 도수
4. 주류 종류
5. 한글 카테고리
6. 영문 카테고리
7. 카테고리 그룹
8. 지역
9. 증류소
10. 숙성 연도
11. 캐스크
12. 설명
13. 용량
14. 테이스팅 태그

설명 규칙:

- `주류 종류`와 `카테고리 그룹`은 한글 표시값을 입력하고 서버의 명시적 vocabulary로 enum에 매핑한다.
- `지역`, `증류소`는 현재 참조 데이터의 한글 이름을 사용한다.
- `테이스팅 태그`는 현재 참조 데이터의 한글 이름을 `|`로 구분한다.
- 이미지 관련 열은 템플릿에서 제외한다.
- 다운로드 템플릿은 입력 행이 비어 있어야 한다. 예시는 `입력 안내` 시트에 둔다.

### 2. 업로드 및 검증

- `POST /v1/alcohols/excel/validate`
- `multipart/form-data`, part 이름은 `file`이다.
- OOXML `.xlsx`만 허용한다. CSV, XLS, ZIP 묶음, 이미지 파일은 제외한다.
- 검증은 DB write 없이 수행한다.
- 기본 방어 한도는 파일 5 MiB, 데이터 1,000행이다. 수식 셀, 외부 링크, 잘못된 시트명, 헤더/설명 행 변경, 중복 헤더, 지원하지 않는 값은 오류 처리한다.
- 완전히 빈 데이터 행은 무시한다.
- 필수 필드, 문자열 공백, enum vocabulary, 지역·증류소·태그 exact 참조, 중복 태그를 검증한다.
- 정규화는 trim, Unicode NFKC, 영문 소문자화, 연속 공백 축약까지만 사용한다. 숫자와 에디션 표기는 제거하지 않는다.
- 파일 내부의 canonical identity `(정규화 이름, 증류소, 도수, 용량)` 중복은 오류다.
- 기존 DB 알코올과 강하게 일치하는 행은 등록 차단 오류가 아니라 `DUPLICATE_CANDIDATE` warning과 후보 ID를 반환한다. 이름 유사도만으로 자동 연결하지 않는다.

검증 응답 최소 구조:

- `totalRows`, `validRows`, `invalidRows`, `warningRows`
- `rows[]`: `rowNumber`, 파싱된 표시값, `valid`, `errors[]`, `warnings[]`
- 각 error/warning: 안정적인 `code`, 한글 `field`, 사용자용 `message`
- 참조 매칭 성공 시 내부 처리/후속 등록용 ID를 행 결과에 포함할 수 있으나, 워크북에는 ID를 요구하지 않는다.

## Image Contract

- 이번 XLSX에는 이미지 열과 이미지 업로드가 없다.
- 기존 `AdminAlcoholUpsertRequest.imageUrl`의 `@NotBlank`를 제거해 생성 시 null/누락을 허용한다.
- 기존 DB 컬럼은 nullable이므로 migration은 없다.
- 수정 요청에서 null은 기존 서비스 규칙대로 기존 이미지를 유지한다. 명시적 이미지 제거 계약은 이번 범위가 아니다.
- OpenAPI와 HTTP 회귀 테스트에서 `imageUrl`이 required 목록에서 빠지는지 검증한다.

## Architecture

- XLSX 생성·파싱은 Admin 모듈에만 둔다. Product와 공용 mono에 spreadsheet 기술 의존성을 전파하지 않는다.
- Controller는 Admin 전용 Excel service 인터페이스에 의존하고, 구현은 Admin 모듈 service에 둔다.
- 참조 조회는 기존 alcohol 도메인의 공개 port/facade 규칙을 따른다. Controller가 repository나 entity를 직접 사용하지 않는다.
- XLSX 라이브러리는 zip bomb, entity expansion, formula/external-link 평가를 비활성화할 수 있는 유지보수 중인 라이브러리를 선택하고 Admin 모듈에만 추가한다.
- 업로드 파일 원문이나 파싱 결과를 DB에 저장하지 않는다.

## Test Strategy

TDD 순서로 다음을 먼저 실패시킨다.

1. 템플릿 workbook이 고정 시트, 1행 한글 필드명, 2행 설명, 이미지 열 제외, 참조 시트와 예시 안내를 가진다.
2. 정상 workbook 업로드가 행과 참조값을 파싱해 valid 결과를 반환한다.
3. 헤더 변경/누락, 설명 행 변경, 잘못된 enum, 없는 지역·증류소·태그, 수식 셀, 행 제한 초과가 안정적인 오류 code를 반환한다.
4. 파일 내부 중복은 오류, 기존 DB 강한 중복은 warning이다.
5. HTTP 다운로드의 content type/content-disposition과 업로드 multipart 계약을 검증한다.
6. 기존 단건 생성에서 imageUrl 누락이 허용되고 상세 응답 imageUrl은 null이다.
7. 기존 단건 수정에서 imageUrl 누락 시 기존 URL이 유지된다.
8. 생성 OpenAPI에서 imageUrl이 optional이고 두 Excel endpoint가 문서화된다.

Mockito interaction 검증은 사용하지 않고, 필요한 참조 의존성은 Fake/InMemory port로 검증한다.

## Non-goals

- 실제 일괄 등록/수정/삭제
- CSV/XLS 지원
- 이미지 업로드, embedded image, ZIP 업로드
- 누락 참조 데이터 자동 생성
- fuzzy 자동 연결
- import batch persistence 또는 migration
- Product API 변경
- push, PR, merge, deploy

## Delivery Artifact

- 구현과 테스트가 완료되면 같은 계약으로 별도 예제 XLSX를 생성한다.
- 예제 파일은 최소 1개의 현실적인 유효 행을 포함하고 formula가 없어야 한다.
- workbook 구조와 셀 값을 독립적으로 다시 읽어 검증한 뒤 현재 Discord 채널에 첨부한다.
