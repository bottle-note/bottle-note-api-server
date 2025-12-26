# Admin REST Docs 작성 규칙

## 테스트 작성 규칙

- `excludeAutoConfiguration = [SecurityAutoConfiguration::class]`로 Security 예외 설정 필수 (문서화 목적이므로 인증 불필요)
- JSON 가독성을 위해 모든 `document()`에 `preprocessRequest(prettyPrint())`, `preprocessResponse(prettyPrint())` 적용 필수
- meta 필드(`serverVersion`, `serverEncoding`, `serverResponseTime`, `serverPathVersion`)는 `.ignored()` 처리
- snippets 경로는 `admin/{도메인}/{기능}` 형식 사용 (예: `admin/auth/login`)

## Asciidoc 문서 작성 규칙

- `[discrete]`는 목차(TOC)에서 제외할 제목에 사용 (하위 섹션 제목, include 앞)
- adoc 파일 경로는 `src/docs/asciidoc/api/admin-{도메인}/{기능}.adoc` 형식
- API 간 구분은 `'''` 사용
- 새 API 문서 추가 시 `admin-api.adoc`에 include 추가 필수

## include 순서

- 요청: `request-fields.adoc` -> `http-request.adoc`
- 응답: `response-fields.adoc` -> `http-response.adoc`
- 쿼리 파라미터: `query-parameters.adoc` -> `curl-request.adoc` -> `http-request.adoc`