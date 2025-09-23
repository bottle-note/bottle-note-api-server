# Core 모듈 마이그레이션 현황

## 🎯 Core 모듈 원칙

### 실무적 정의

```
"bottlenote-core는 핵심 비즈니스 로직과 도메인 모델"
- JPA 엔티티 및 도메인 객체
- Service/Facade 비즈니스 로직
- Repository 인터페이스 (Port)
- 도메인 이벤트
- 외부 연동 Port 인터페이스
```

---

## 📊 현재 Core 모듈 상태

### 현재 구조 (거의 비어있음)
```
bottlenote-core/
└── src/main/java/app/bottlenote/core/
    ├── application/  (비어있음)
    ├── domain/       (비어있음)
    ├── port/         (비어있음)
    └── structure/   (지울 예정)
        ├── Pair.java
        └── Triple.java
```

### 현재 보유 컴포넌트
- `Pair<L, R>` - 두 개의 값을 담는 데이터 구조 ✅
- `Triple<L, M, R>` - 세 개의 값을 담는 데이터 구조 ✅

---

## 🔄 Legacy → Core 이관 필수 항목

### ✅ Phase 1: 기반 엔티티 (최우선)

1. **Base 엔티티**
   - `BaseEntity` - 모든 엔티티의 기반 (createdAt, updatedAt, deletedAt)
   - `BaseTimeEntity` - 시간 정보만 있는 기반 엔티티

### ✅ Phase 2: 도메인별 이관 (우선순위 순)

#### 1. User 도메인 (인증/인가 핵심)
**엔티티:**
- `User`, `UserProfile`, `UserDevice`
- `Follow`, `FollowId`
- `RefreshToken`, `OauthInfo`

**서비스/파사드:**
- `UserFacade`, `DefaultUserFacade`
- `AuthService`, `UserBasicService`
- `OauthService`, `KakaoAuthService`, `AppleAuthService`
- `FollowService`, `FollowFacade`
- `NonceService`

**Repository 인터페이스:**
- `UserRepository`, `FollowRepository`
- `RefreshTokenRepository`, `OauthInfoRepository`

#### 2. Alcohols 도메인 (핵심 도메인)
**엔티티:**
- `Alcohol`, `AlcoholImages`
- `AlcoholCategory`, `Region`

**서비스:**
- `AlcoholQueryService`
- `AlcoholCommandService`

**Repository 인터페이스:**
- `AlcoholRepository`
- `AlcoholCategoryRepository`

#### 3. Review 도메인
**엔티티:**
- `Review`, `ReviewImages`
- `ReviewReply`, `ReviewTag`

**서비스:**
- `ReviewQueryService`
- `ReviewCommandService`

**Repository 인터페이스:**
- `ReviewRepository`
- `ReviewReplyRepository`

#### 4. Rating 도메인
**엔티티:**
- `Rating`, `RatingPoint`

**서비스:**
- `RatingQueryService`
- `RatingCommandService`

**이벤트:**
- `RatingRegistryEvent`

**Repository 인터페이스:**
- `RatingRepository`

#### 5. Picks 도메인
**엔티티:**
- `Picks`, `PicksId`

**서비스:**
- `PicksCommandService`

**이벤트:**
- `PicksRegistryEvent`

**Repository 인터페이스:**
- `PicksRepository`

#### 6. Like 도메인
**엔티티:**
- `Likes`, `LikesId`

**서비스:**
- `LikesCommandService`

**이벤트:**
- `LikesRegistryEvent`

**Repository 인터페이스:**
- `LikesRepository`

#### 7. History 도메인
**엔티티:**
- `UserHistory`, `UserHistoryDetail`

**서비스:**
- `UserHistoryService`

**Repository 인터페이스:**
- `UserHistoryRepository`

#### 8. Support 도메인
**엔티티:**
- `Help`, `HelpCategory`
- `UserReport`, `ReviewReport`
- `Block`, `BlockType`
- `BusinessSupport`

**서비스:**
- `HelpService`
- `UserReportService`, `ReviewReportService`
- `BlockService`
- `BusinessSupportService`

**Repository 인터페이스:**
- `HelpRepository`
- `ReportRepository`
- `BlockRepository`

### ✅ Phase 3: Port 인터페이스 정의

#### 외부 연동 Port (Infrastructure에서 구현)
```java
// 인증 관련
public interface ExternalAuthPort {
    OAuthUserInfo fetchKakaoUser(String token);
    AppleUserInfo fetchAppleUser(String token);
}

// 파일 업로드
public interface FileUploadPort {
    String generatePresignedUrl(String key);
    void deleteFile(String key);
}

// 알림 발송
public interface NotificationPort {
    void sendPushNotification(String deviceToken, String message);
    void sendBulkNotification(List<String> tokens, String message);
}

// 캐시
public interface CachePort {
    void set(String key, Object value, Duration ttl);
    Optional<Object> get(String key);
    void delete(String key);
}
```

### ✅ Phase 4: 도메인 이벤트

**이벤트 Publisher 인터페이스:**
```java
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
```

**도메인 이벤트들:**
- `RatingRegistryEvent`
- `PicksRegistryEvent`
- `LikesRegistryEvent`
- `ReviewCreatedEvent`
- `UserRegisteredEvent`
- `S3RequestEvent`

---

## 📋 마이그레이션 체크리스트

### Phase 1: 기반 구축 (1주)
- [ ] BaseEntity, BaseTimeEntity 이관
- [ ] 기본 Port 인터페이스 정의
- [ ] DomainEventPublisher 인터페이스 정의
- [ ] 공통 도메인 예외 정의

### Phase 2: User 도메인 (1주)
- [ ] User 관련 엔티티 이관
- [ ] AuthService, UserService 이관
- [ ] UserFacade 이관
- [ ] Repository 인터페이스 정의
- [ ] 테스트 코드 이관

### Phase 3: 핵심 도메인 (2주)
- [ ] Alcohols 도메인 완전 이관
- [ ] Review 도메인 완전 이관
- [ ] Rating 도메인 완전 이관
- [ ] 도메인 간 의존성 정리

### Phase 4: 부가 도메인 (1주)
- [ ] Picks, Like 도메인 이관
- [ ] History 도메인 이관
- [ ] Support 도메인 이관

### Phase 5: 검증 및 최적화 (1주)
- [ ] 순환 의존성 검증
- [ ] 아키텍처 테스트 추가
- [ ] 성능 테스트
- [ ] 문서화

---

## 🔧 Core 모듈 build.gradle 의존성

### 현재 상태 (최소 의존성)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 필요한 의존성 추가
```gradle
dependencies {
    // Shared 모듈 의존
    implementation project(':bottlenote-shared')

    // Spring Boot 기본
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // JPA/Hibernate (엔티티 정의용)
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.hibernate:hibernate-core'
    implementation 'jakarta.persistence:jakarta.persistence-api'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Jackson (DTO 직렬화)
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'

    // Spring Context (이벤트, DI)
    implementation 'org.springframework:spring-context'
    implementation 'org.springframework:spring-tx'

    // Apache Commons (유틸리티)
    implementation 'org.apache.commons:commons-lang3'
    implementation 'org.apache.commons:commons-collections4'

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'org.assertj:assertj-core'

    // 테스트용 H2 DB
    testRuntimeOnly 'com.h2database:h2'
}
```

### QueryDSL 설정 (엔티티용, Q클래스는 Infrastructure로)
```gradle
// QueryDSL 버전
def queryDslVersion = '5.0.0'

dependencies {
    // QueryDSL JPA
    implementation "com.querydsl:querydsl-jpa:${queryDslVersion}:jakarta"
    annotationProcessor "com.querydsl:querydsl-apt:${queryDslVersion}:jakarta"
    annotationProcessor "jakarta.annotation:jakarta.annotation-api"
    annotationProcessor "jakarta.persistence:jakarta.persistence-api"
}

// QueryDSL 설정
def querydslDir = "$buildDir/generated/querydsl"

sourceSets {
    main.java.srcDirs += querydslDir
}

tasks.withType(JavaCompile) {
    options.annotationProcessorGeneratedSourcesDirectory = file(querydslDir)
}

clean.doLast {
    file(querydslDir).deleteDir()
}
```

### 의존성 주의사항
1. **Infrastructure 의존 금지**
   - Feign, Redis, AWS SDK 등 외부 연동 라이브러리 제외
   - 이들은 Infrastructure 모듈에만 위치

2. **Shared 모듈 의존 필수**
   - JWT, 공통 DTO, 유틸리티 사용을 위해

3. **테스트 의존성**
   - 단위 테스트용 의존성만 포함
   - TestContainers 등 통합 테스트는 Infrastructure에서

---

## ⚠️ 주의사항

### 의존성 규칙
1. **Core → Shared만 의존**
   - Infrastructure 직접 의존 금지
   - Port 인터페이스를 통한 의존성 역전

2. **Facade 패턴 유지**
   - 도메인 간 통신은 Facade 통해서만
   - Service 직접 호출 금지

3. **엔티티 이관 시**
   - QueryDSL Q클래스는 Infrastructure로
   - 엔티티 본체만 Core로
   - Repository 구현체는 Infrastructure로

4. **테스트 우선**
   - 단위 테스트 함께 이관
   - 통합 테스트는 Infrastructure에서

---

## 📊 마이그레이션 요약

### Legacy → Core 이관 대상

**총 규모:**
- 12개 주요 도메인
- 약 50개 엔티티 클래스
- 약 30개 Service/Facade 클래스
- 약 20개 Repository 인터페이스
- 약 10개 Port 인터페이스 (새로 정의)
- 약 6개 도메인 이벤트

### 예상 소요 시간
- 총 6주 (1.5개월)
- 주간 단위 단계적 이관
- 각 단계별 검증 포함

### 현재 진행 상태
- ⬜ 미시작 (Core 모듈 거의 비어있음)
- 🔄 Shared 모듈 마이그레이션과 병행 필요

---

*최종 수정: 2025-09-12*
*핵심 비즈니스 로직 중심의 실무적 접근*
