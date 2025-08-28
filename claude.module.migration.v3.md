# 멀티모듈 마이그레이션 가이드 v3

## 🎯 목표

- 기존 모노리스를 멀티모듈로 전환
- Product API와 Admin API 분리
- 단계적이고 안전한 마이그레이션

## 📁 모듈 구조

```
bottlenote/
├── bottlenote-shared/         # 공통 컴포넌트 (JWT, DTO, Utils)
├── bottlenote-core/            # 엔티티, 서비스, 파사드 (도메인+애플리케이션)
├── bottlenote-infrastructure/ # JPA 구현체, 외부 연동
├── bottlenote-product-api/    # 사용자 API (30001)
├── bottlenote-admin-api/      # 관리자 API (30100, Kotlin)
└── bottlenote-legacy/         # 임시 보관용 (최종 제거 예정)
```

## 🔗 의존성 구조

### 의존성 방향

- **product-api**: core 의존
- **admin-api**: core 의존
- **core**: infrastructure, shared 의존 (api로 shared 전파)
- **infrastructure**: core 의존
- **shared**: 독립 (의존성 없음)

## 📋 각 모듈 역할

### bottlenote-shared

- 스프링 의존성이 아닌 순수 공유 컴포넌트
- JWT Provider, Token Validator
- Request/Response DTO
- 유틸리티 클래스
- 공통 상수

### bottlenote-core

- core 이름 선택 이유:
	- ✅ 핵심 비즈니스 + 로직 모두 포함하는 중립적 이름
	- ✅ 도메인과 애플리케이션 구분 없이 핵심이라는 의미
	- ✅ 추후 필요시 domain/application으로 분리 가능
- JPA 엔티티
- Repository 인터페이스
- Service / Facade 클래스
	- **Service**: 단일 도메인의 비즈니스 로직, Controller와 직접 통신
	- **Facade**: 도메인 간 격벽 연결, 다른 도메인 접근 인터페이스
	- 예: ReviewService가 UserFacade를 통해 User 도메인 정보 획득
- 도메인 이벤트
	- 추후 SpringEventPublisher, KafkaEventPublisher 등 다양한 구현체로 확장 가능
- BaseEntity
- 비즈니스 로직

### bottlenote-infrastructure

- JPA Repository 구현체
- QueryDSL 구현
- 외부 API 클라이언트
- Redis, AWS, Firebase 연동

### bottlenote-product-api

- 사용자용 REST Controller
- SecurityConfig
- 30001 포트
- 추후 Legacy 모듈의 이름을 변경해서 통합 예정.

### bottlenote-admin-api

- 관리자용 REST Controller (Kotlin)
- 별도 SecurityConfig
- 30100 포트
- 별도 파이프라인 구축

---

*최종 수정: 2025-08-27*
