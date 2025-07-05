# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 유저 지침

- 영어로 질문하는 경우 의도를 잘 파악하고 문법적 오류가 있을 경우 응답에 문법에 대한 피드백을 제공하세요.
- 질문 언어와 관련없이 응답 언어는 한국어로 작성하세요.
- 복잡하거나 애매한 질문의 경우 이해한 내용을 요약해서 응답에 포함하세요.
- 질문에 대한 답변은 명확하고 간결하게 작성하세요 (3-5줄 내외 권장).
- 코드 예시가 필요한 경우 전체 코드를 한번에 보여줄지, 단계별로 나누어 설명할지 사용자에게 물어보세요.
- 코드 예시가 필요한 경우, 코드의 목적과 사용법을 간단히 설명하세요.
- 코드를 작성할 경우 주석은 한줄로 간략하게만 작성하세요.
- 기존 코드 수정 시 프로젝트의 기존 패턴과 컨벤션을 반드시 따르세요.
- 여러 파일 수정이 필요한 경우 수정할 파일 목록을 미리 알려주세요.
- 에러나 문제 해결 시 단계별 접근 방법을 제시하세요.
- 여러 해결책이 있을 때는 프로젝트 컨텍스트에 가장 적합한 방법을 우선 추천하세요.

## 프로젝트 개요

- **기술 스택**: Spring Boot 3.1.9, Java 21, MySQL, Redis, QueryDSL
- **아키텍처**: 도메인 주도 설계(DDD) 기반 멀티모듈 구조
- **주요 도메인**: alcohols, user, review, rating, support, history, picks, like

## 빌드 및 실행

```bash
./gradlew build                 # 전체 빌드
./gradlew test                  # 단위 테스트
./gradlew integration_test      # 통합 테스트
./gradlew check_rule_test       # 아키텍처 규칙 테스트
./gradlew bootRun               # 애플리케이션 실행
./gradlew asciidoctor           # API 문서 생성
```

## 코드 작성 규칙

### 아키텍처 패턴

- **계층 구조**: Controller → Facade → Service → Repository → Domain
- **도메인별 패키지**: constant, controller, domain, dto, repository, service, facade, exception, event

### 네이밍 컨벤션

- **클래스**: `{도메인명}Controller`, `Default{도메인명}Facade`, `Jpa{도메인명}Repository`, `{도메인명}Exception`
- **메서드**: get/find/search (조회), create/register (생성), update/modify/change (수정), delete/remove (삭제)

### 프로젝트 특화 어노테이션

- `@FacadeService`: 퍼사드 서비스 계층
- `@DomainEventListener`: 도메인 이벤트 리스너
- `@DomainRepository`: 도메인 레포지토리
- `@JpaRepositoryImpl`: JPA 레포지토리 구현체
- `@ThirdPartyService`: 외부 서비스

### 예외 처리

- 도메인별 예외: `{도메인명}Exception`, `{도메인명}ExceptionCode`
- 전역 예외 핸들러: `@RestControllerAdvice`
- 통일된 응답: `GlobalResponse`

### 코드 스타일

- Lombok: `@Getter`, `@Builder`, `@RequiredArgsConstructor`
- 불변성: `record` 사용 (DTO), `final` 필드 선호
- 페이징: `PageResponse`, `CursorPageable`

## 테스트 작성 규칙

### 테스트 분류 및 네이밍

- `@Tag("unit")`: 단위 테스트, `@Tag("integration")`: 통합 테스트, `@Tag("rule")`: 아키텍처 규칙
- 클래스명: `{기능명}ServiceTest`, 메서드명: `{기능명}할_수_있다`
- `@DisplayName`: 한글로 테스트 목적 명시

### 테스트 구조

- Given-When-Then 패턴 사용
- Fixture 클래스를 통한 테스트 데이터 관리
- TestContainers 사용 (실제 DB 환경)
- 테스트 데이터: `src/test/resources/init-script/` 디렉토리

## 데이터베이스 설계

### JPA 엔티티

- `BaseEntity` 상속 (공통 필드)
- 복합 키: `@Embeddable` 사용
- 엔티티 필터링: Hibernate `@Filter` 활용

### QueryDSL 패턴

- `Custom{도메인명}Repository` 인터페이스 + `Custom{도메인명}RepositoryImpl` 구현체
- 동적 쿼리: BooleanBuilder 또는 조건부 where 절
- 성능 최적화: 페치 조인, @BatchSize, `@Cacheable`

## 보안 및 인증

- JWT 토큰: 액세스 토큰 24시간, 리프레시 토큰 30일
- 토큰 검증: `JwtTokenProvider`, 보안 설정: `SecurityConfig`
- API 보안: `@PreAuthorize` 또는 `@Secured`, CORS: `WebConfig`

## 외부 서비스 연동

- OpenFeign: `@FeignClient`, 설정 분리 `FeignConfig`, 에러 처리 `ErrorDecoder`
- AWS S3: PreSigned URL 생성, `AwsS3Config`
- Firebase FCM: `FirebaseProperties`, 비동기 처리 `@Async`

## 좋은 Spring Boot 개발 관습

### 응답 통일성

- API 응답 형식 통일: `GlobalResponse` 또는 `ResponseEntity` 일관성 유지
- 에러 응답 표준화: HTTP 상태 코드와 에러 메시지 일관성

### 성능 최적화

- N+1 문제 방지: 페치 조인, `@BatchSize`, 쿼리 최적화
- 캐싱 전략: `@Cacheable` 적절히 활용
- 비동기 처리: `@Async`, 이벤트 기반 처리

### 보안 기본 원칙

- 입력값 검증: `@Valid`, `@Validated` 사용
- 민감 정보 로깅 금지
- SQL 인젝션 방지: PreparedStatement 사용

### 테스트 품질

- 단위 테스트와 통합 테스트 분리
- 테스트 데이터 격리: 각 테스트 독립성 보장
- Mock 적절히 활용: 외부 의존성 분리

### 코드 품질

- 의존성 주입: 생성자 주입 우선
- 불변성 지향: `final` 필드, `record` 활용
- 단일 책임 원칙: 클래스와 메서드 역할 명확화
