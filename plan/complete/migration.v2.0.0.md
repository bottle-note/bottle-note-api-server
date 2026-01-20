```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2025-10-09

** Core Achievements **
- Top-down 최소 분리 전략으로 순환 의존성 문제 해결
- bottlenote-product-api 모듈 생성 (웹 진입점)
- bottlenote-mono 모듈 생성 (비즈니스 로직)
- Dockerfile, docker-compose, CI/CD 파이프라인 구축

** Key Components **
- bottlenote-product-api: SecurityConfig, 웹 필터
- bottlenote-mono: DTO, Service, Facade, Repository, Exception

** Key Principle **
- 단방향 의존성 확보: product-api → mono
- DTO는 mono에 유지 (순환 의존 방지)
================================================================================
```

# 모듈 마이그레이션 v2 - 최소 웹 계층 분리 전략

## 📋 배경 및 문제점

### 기존 마이그레이션의 문제
- **순환 의존성 지옥**: legacy → core/shared 분리 시 상호 참조 발생
- **DTO 중첩 의존**: DTO 레벨에서도 복잡한 순환 참조 문제
- **Bottom-up의 한계**: 하위 계층부터 분리하니 의존성이 꼬임

#### 실제 예시: RatingPoint 순환 의존성
```
문제 상황:
1. RatingPoint (도메인 VO) → rating 패키지
2. RatingPointConverter (웹 변환기) → global 패키지
3. WebConfig (웹 설정) → Converter 등록
4. RatingException → rating 패키지

분리 시도 시 발생하는 순환:
- RatingPoint를 Core로 이동
  → RatingException도 함께 이동 필요
  → RatingPointConverter는 웹 계층이라 Legacy에 남음
  → Converter가 Core의 클래스들 참조 (Legacy → Core)
  → WebConfig가 Converter 필요 (순환 의존 시작)

- Converter를 Shared로 이동
  → WebConfig가 Shared 의존 (Legacy → Shared)
  → Converter가 Core 의존 (Shared → Core)
  → Core가 Shared 유틸 사용 시 (Core → Shared) ❌ 순환!
```

이처럼 웹 계층 Converter가 도메인 객체와 예외를 모두 알아야 하는 구조에서
모듈 분리 시 필연적으로 순환 참조가 발생합니다.

### 새로운 접근: Top-down 최소 분리
- 웹 계층만 최소한으로 추출
- DTO는 mono에 유지 (순환 의존 방지)
- 단방향 의존성 확보: `product-api → mono`

## 🏗️ 목표 모듈 구조

```
bottle-note-api-server/
├── bottlenote-product-api/  # 웹 진입점만
├── bottlenote-mono/          # 모든 비즈니스 로직
├── bottlenote-batch/         # 배치 (유지)
└── bottlenote-core/          # (제거 예정 - 주석 처리)
```

## 🎯 모듈별 역할 정의

### bottlenote-product-api (최소 웹 계층)
**포함되는 것 (최소한만):**
- ✅ 컨트롤러 클래스들 (24개)
- ✅ `@RestControllerAdvice` (GlobalExceptionHandler)
- ✅ Spring Boot Main 클래스
- ✅ 웹 필터 (JwtAuthenticationFilter)
- ✅ 웹 설정 (SecurityConfig, WebConfig)

**포함되지 않는 것:**
- ❌ DTO (mono에 유지)
- ❌ Service/Facade (mono에 유지)
- ❌ Exception 정의 (mono에 유지)
- ❌ 비즈니스 로직

### bottlenote-mono (핵심 비즈니스)
**이름 대안:**
- `bottlenote-core-business`
- `bottlenote-domain`
- `bottlenote-foundation`

**포함되는 것:**
- ✅ 모든 도메인 패키지 (alcohols, user, review 등)
- ✅ 모든 DTO (request/response 포함)
- ✅ Service/Facade/Repository
- ✅ Domain/Entity
- ✅ Exception 정의
- ✅ 공통 유틸리티
- ✅ JPA/DB 설정
- ✅ Redis 설정

## 📁 디렉토리 구조 변경 계획

### 현재 (bottlenote-legacy)
```
bottlenote-legacy/
└── src/main/java/app/
    ├── bottlenote/
    │   ├── alcohols/
    │   │   ├── controller/   → product-api로 이동
    │   │   ├── service/      → mono 유지
    │   │   ├── dto/          → mono 유지
    │   │   └── ...
    │   └── user/
    │       ├── controller/   → product-api로 이동
    │       └── ...
    └── BottleNoteApplication.java → product-api로 이동
```

### 변경 후
```
bottlenote-product-api/
└── src/main/java/app/
    ├── bottlenote/
    │   └── api/
    │       ├── alcohols/     # 컨트롤러만
    │       ├── user/         # 컨트롤러만
    │       ├── config/       # SecurityConfig, WebConfig
    │       └── handler/      # GlobalExceptionHandler
    └── BottleNoteApiApplication.java

bottlenote-mono/
└── src/main/java/app/
    └── bottlenote/
        ├── alcohols/         # 컨트롤러 제외 모든 것
        ├── user/            # 컨트롤러 제외 모든 것
        └── ...
```

## 🔧 build.gradle 의존성 계획

### bottlenote-product-api
```gradle
dependencies {
    // mono 모듈 의존
    implementation project(':bottlenote-mono')

    // 웹 관련만
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
}

bootJar {
    enabled = true  // 실행 가능한 JAR
}
```

### bottlenote-mono
```gradle
dependencies {
    // 웹 의존성 제외
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    // ... 기타 비즈니스 로직 의존성
}

bootJar {
    enabled = false  // 라이브러리 JAR
}
jar {
    enabled = true
}
```

## 🚀 실행 계획 (순서 중요)

### Phase 1: 준비
1. 현재 상태 백업 (브랜치 생성)
2. bottlenote-core 모듈 주석 처리 계획

### Phase 2: 모듈 생성
1. `bottlenote-product-api` 모듈 생성
2. `bottlenote-legacy` → `bottlenote-mono` 이름 변경
3. settings.gradle 수정

### Phase 3: 의존성 정리
1. mono의 build.gradle에서 web 의존성 제거
2. product-api의 build.gradle 설정

### Phase 4: 코드 이동 (최소한만)
1. 컨트롤러 클래스들만 product-api로 이동
2. Main 클래스 이동
3. 웹 설정 클래스 이동
4. 패키지 구조 정리

### Phase 5: 검증
1. 컴파일 테스트
2. 의존성 순환 확인
3. 애플리케이션 실행 테스트

## ⚠️ 주의사항

1. **DTO 이동 금지**: 순환 의존성 방지를 위해 mono에 유지
2. **최소 추출 원칙**: 웹 진입점만 분리
3. **패키지 경로 유지**: 가능한 한 import 변경 최소화
4. **점진적 접근**: 추후 mono를 더 작은 모듈로 분리 가능

## 📝 TODO Checklist

- [ ] bottlenote-core 제거 계획 수립
- [ ] 모듈 이름 최종 결정
- [ ] 패키지 구조 세부 설계
- [ ] 이동할 클래스 목록 작성
- [ ] 테스트 전략 수립

---

작성일: 2025-09-28
기준 커밋: 460c25f1 (fix: enhance API documentation generation in GitHub Actions workflow)
브랜치: restore-legacy-module-base
상태: 계획 단계
