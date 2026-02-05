# GitHub Copilot 저장소 지침

## 응답 언어

- 모든 응답은 **한국어**로 작성하세요.
- 코드 주석도 한국어로 간략하게 작성하세요.

## 코드 리뷰 지침

- **중요도 높음(High severity)** 이슈만 리뷰하세요.
- 다음 항목에 집중하세요:
  - 보안 취약점 (SQL Injection, XSS 등)
  - 잠재적 버그 (NPE, 무한 루프 등)
  - 성능 문제 (N+1, 메모리 누수 등)
  - 아키텍처 규칙 위반
- 코드 스타일, 네이밍, 포맷팅 등 낮은 중요도 이슈는 무시하세요.

## 프로젝트 컨텍스트

- **기술 스택**: Spring Boot 3.x, Java 21, Kotlin, MySQL, Redis, QueryDSL
- **아키텍처**: 멀티모듈 (mono, admin-api, product-api)
- **테스트**: JUnit 5, TestContainers, MockMvc
- 자세한 프로젝트 규칙은 `CLAUDE.md` 참고
