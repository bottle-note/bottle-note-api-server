# 삭제 예정 — RestDocs 기반 문서화

이 디렉터리의 문서화 테스트는 **삭제 예정**입니다. 새 엔드포인트를 추가할 때 여기에 테스트를 추가하지 마세요.

## 대체 수단

API 문서는 springdoc OpenAPI 어노테이션으로 생성합니다.

- 스펙: `GET /openapi.product.json` (앱 실행 중 조회)
- 문서 작성 위치: `app.bottlenote.{도메인}.controller.docs.{컨트롤러}ApiDocs`
- 품질 검증: `OpenApiSpecQualityTest` — 태그·요약·응답 스키마 누락을 전수 검사합니다

새 엔드포인트를 만들면 해당 도메인의 `*ApiDocs`에 설명을 추가하고 컨트롤러에 어노테이션을 한 줄 붙이면 됩니다. 문서화를 빠뜨리면 `OpenApiSpecQualityTest`가 실패합니다.

## 함께 삭제될 자산

| 자산 | 위치 |
|---|---|
| 문서화 테스트 | `bottlenote-product-api/src/test/java/app/docs/**`, `app/external/docs/**` |
| AsciiDoc 원본 | `bottlenote-product-api/src/docs/asciidoc/**` |
| Antora 사이트 | 저장소 루트 `docs/` |
| 의존성 | `spring-restdocs`, `restdocs-api-spec` (`gradle/libs.versions.toml`) |
| Gradle 태스크 | `asciidoctor`, `restDocsTest`, `openapi3` |
| 워크플로 | GitHub Pages 배포 (있는 경우) |

## 왜 아직 남겨두는가

두 문서 체계를 한 번에 갈아치우면 전환 기간에 문서 공백이 생깁니다. 클라이언트가 새 스펙으로 옮겨갈 시간을 두기 위해 당분간 양쪽을 함께 유지합니다. 두 체계 모두 CI에서 계속 통과해야 합니다.

실제 제거 시점과 조건은 `plan/openapi-annotation-docs.md`의 가정 3·4를 참고하세요.
