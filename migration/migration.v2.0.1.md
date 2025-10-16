# 모듈 마이그레이션 v2.0.1 - 컨트롤러 분리

## 배경

### v2.0.0 정리
- **성공**: Top-down 최소 분리 전략으로 순환 의존성 회피
- **성공**: product-api 모듈 생성 및 SecurityConfig 분리
- **성공**: Dockerfile, docker-compose, CI/CD 파이프라인 구축
- **성공**: 배포 환경 검증 완료 (development/production)

### v2.0.1 목표
**컨트롤러 24개만 product-api로 이동**

v2.0.0 원칙 유지:
- DTO는 mono에 유지 (순환 의존 방지)
- GlobalExceptionHandler는 mono에 유지 (예외 처리 중앙화)
- WebConfig는 mono에 유지 (Converter 등록)
- **컨트롤러만 최소 이동**

---

## 현재 상태 (v2.0.0 완료)

### 모듈 구조

```
bottle-note-api-server/
├── bottlenote-product-api/  # 웹 진입점 (일부 완료)
│   ├── ProdcutApplication.java  (오타: Prodcut → Product 수정 필요)
│   ├── SecurityConfig           (JWT 인증, CORS 설정)
│   ├── build.gradle             (mono 의존, bootJar 활성화)
│   └── application.yml          (포트 8080, 실제 배포: 30001)
│
├── bottlenote-mono/          # 모든 비즈니스 로직
│   ├── 24개 컨트롤러           (이동 예정)
│   ├── GlobalExceptionHandler  (mono에 유지)
│   ├── WebConfig               (mono에 유지)
│   ├── DTO (request/response)  (mono에 유지)
│   ├── Service/Facade          (mono에 유지)
│   ├── Repository              (mono에 유지)
│   └── Exception 정의          (mono에 유지)
│
├── bottlenote-admin-api/     # 관리자 API (미개발)
└── bottlenote-batch/         # 배치 (유지)
```

### 의존성 구조

```
product-api → mono (단방향)

bootJar:
- product-api: enabled (실행 가능한 JAR)
- mono: disabled (라이브러리 JAR)
```

### 배포 구조

**Dockerfile**
```dockerfile
# bottlenote-product-api.jar를 빌드
COPY --from=builder /app/bottlenote-product-api/build/libs/bottlenote-product-api.jar /app.jar
```

**docker-compose (development)**
- 포트: 30001:30001
- 프로필: dev
- 메모리: 768m

**CI/CD**
- deploy_development.yml (dev 브랜치 merge 시)
- deploy_production.yml (운영 배포)
- continuous_integration.yml (PR 검증)

---

## v2.0.1 목표 상세

### 이동 대상: 컨트롤러 24개

#### 1. Alcohols 도메인 (4개)
```
mono → product-api

bottlenote-mono/src/main/java/app/bottlenote/alcohols/presentation/
├── AlcoholQueryController.java
├── AlcoholPopularQueryController.java
├── AlcoholReferenceController.java
└── AlcoholExploreController.java
```

#### 2. User 도메인 (5개)
```
bottlenote-mono/src/main/java/app/bottlenote/user/presentation/
├── AuthV2Controller.java
├── FollowController.java
├── UserBasicController.java
├── OauthController.java
└── UserMyPageController.java
```

#### 3. Review 도메인 (3개)
```
bottlenote-mono/src/main/java/app/bottlenote/review/presentation/
├── ReviewController.java
├── ReviewReplyController.java
└── ReviewExploreController.java
```

#### 4. Support 도메인 (4개)
```
bottlenote-mono/src/main/java/app/bottlenote/support/*/presentation/
├── BusinessSupportController.java
├── ReportCommandController.java
├── HelpCommandController.java
└── BlockController.java
```

#### 5. External 도메인 (3개)
```
bottlenote-mono/src/main/java/app/external/*/presentation/
├── NotificationController.java
├── PushController.java
└── AppInfoController.java
```

#### 6. 기타 도메인 (5개)
```
├── picks/presentation/PicksCommandController.java
├── like/presentation/LikesCommandController.java
├── rating/presentation/RatingController.java
├── history/presentation/UserHistoryController.java
└── common/file/presentation/ImageUploadController.java
```

### mono에 유지 (v2.0.0 원칙)

**웹 계층 공통 컴포넌트:**
- `GlobalExceptionHandler` (`@RestControllerAdvice`)
  - 모든 예외 처리 중앙화
  - mono에서 product-api의 컨트롤러 예외 처리

- `WebConfig` (WebMvcConfigurer)
  - RatingPointConverter 등록
  - Converter는 mono에 유지 (순환 의존 방지)

**비즈니스 계층:**
- 모든 DTO (request/response)
- 모든 Service/Facade
- 모든 Repository
- 모든 Exception 정의
- 모든 Converter

---

## 목표 디렉토리 구조

### product-api (컨트롤러만)

```
bottlenote-product-api/
└── src/main/java/app/
    ├── ProductApplication.java  (오타 수정)
    ├── global/
    │   └── security/
    │       └── SecurityConfig.java
    └── bottlenote/
        ├── alcohols/
        │   └── presentation/
        │       ├── AlcoholQueryController.java
        │       ├── AlcoholPopularQueryController.java
        │       ├── AlcoholReferenceController.java
        │       └── AlcoholExploreController.java
        ├── user/
        │   └── presentation/
        │       ├── AuthV2Controller.java
        │       ├── FollowController.java
        │       ├── UserBasicController.java
        │       ├── OauthController.java
        │       └── UserMyPageController.java
        ├── review/
        │   └── presentation/
        │       ├── ReviewController.java
        │       ├── ReviewReplyController.java
        │       └── ReviewExploreController.java
        ├── support/
        │   ├── business/presentation/
        │   ├── report/presentation/
        │   ├── help/presentation/
        │   └── block/presentation/
        ├── external/
        │   ├── notification/presentation/
        │   ├── push/presentation/
        │   └── version/presentation/
        ├── picks/presentation/
        ├── like/presentation/
        ├── rating/presentation/
        ├── history/presentation/
        └── common/file/presentation/
```

### mono (비즈니스 로직 유지)

```
bottlenote-mono/
└── src/main/java/app/bottlenote/
    ├── global/
    │   ├── exception/
    │   │   └── handler/
    │   │       └── GlobalExceptionHandler.java  (유지)
    │   ├── config/
    │   │   └── WebConfig.java  (유지)
    │   └── service/converter/
    │       └── RatingPointConverter.java  (유지)
    ├── alcohols/
    │   ├── dto/  (유지)
    │   ├── service/  (유지)
    │   ├── facade/  (유지)
    │   └── repository/  (유지)
    └── ... (모든 도메인 동일 구조)
```

---

## 실행 계획

### Phase 1: 준비 (30분)
1. 현재 상태 백업 (브랜치 생성)
2. ProdcutApplication → ProductApplication 오타 수정
3. 이동 대상 컨트롤러 24개 최종 확인

### Phase 2: 컨트롤러 이동 (도메인별 순차)

#### Step 1: Alcohols 도메인 (4개)
```bash
# 디렉토리 생성
mkdir -p bottlenote-product-api/src/main/java/app/bottlenote/alcohols/presentation

# 파일 이동
mv bottlenote-mono/src/main/java/app/bottlenote/alcohols/controller/*.java \
   bottlenote-product-api/src/main/java/app/bottlenote/alcohols/presentation/
```

**검증:**
- [ ] import 경로 확인 (DTO는 mono 참조)
- [ ] 컴파일 성공
- [ ] 테스트 실행

#### Step 2: User 도메인 (5개)
```bash
mkdir -p bottlenote-product-api/src/main/java/app/bottlenote/user/presentation
mv bottlenote-mono/src/main/java/app/bottlenote/user/controller/*.java \
   bottlenote-product-api/src/main/java/app/bottlenote/user/presentation/
```

#### Step 3: Review 도메인 (3개)
```bash
mkdir -p bottlenote-product-api/src/main/java/app/bottlenote/review/presentation
mv bottlenote-mono/src/main/java/app/bottlenote/review/controller/*.java \
   bottlenote-product-api/src/main/java/app/bottlenote/review/presentation/
```

#### Step 4: Support 도메인 (4개)
```bash
# support는 하위 디렉토리 구조 유지
mkdir -p bottlenote-product-api/src/main/java/app/bottlenote/support/{business,report,help,block}/presentation

# 각각 이동
mv bottlenote-mono/src/main/java/app/bottlenote/support/business/controller/*.java \
   bottlenote-product-api/src/main/java/app/bottlenote/support/business/presentation/
# ... (나머지 동일)
```

#### Step 5: External 도메인 (3개)
```bash
mkdir -p bottlenote-product-api/src/main/java/app/external/{notification,push,version}/presentation

mv bottlenote-mono/src/main/java/app/external/notification/presentation/*.java \
   bottlenote-product-api/src/main/java/app/external/notification/presentation/
# ... (나머지 동일)
```

#### Step 6: 기타 도메인 (5개)
```bash
mkdir -p bottlenote-product-api/src/main/java/app/bottlenote/{picks,like,rating,history}/presentation
mkdir -p bottlenote-product-api/src/main/java/app/bottlenote/common/file/presentation

# 각각 이동
```

### Phase 3: 검증 (각 Step마다)

1. **컴파일 검증**
```bash
./gradlew :bottlenote-product-api:build -x test
./gradlew :bottlenote-mono:build -x test
```

2. **Import 경로 확인**
- DTO import는 `app.bottlenote.*.dto.*` (mono 모듈)
- Service import는 `app.bottlenote.*.service.*` (mono 모듈)
- 컨트롤러 간 import 없음 확인

3. **테스트 실행**
```bash
./gradlew test
./gradlew integration_test
```

### Phase 4: 최종 검증 (모든 이동 완료 후)

1. **빌드 테스트**
```bash
./gradlew clean build
```

2. **JAR 생성 확인**
```bash
ls -lh bottlenote-product-api/build/libs/bottlenote-product-api.jar
```

3. **로컬 실행 테스트**
```bash
./gradlew :bottlenote-product-api:bootRun
```

4. **배포 검증 (Docker)**
```bash
docker build -t test-product-api .
docker run -p 30001:30001 test-product-api
curl http://localhost:30001/api/v1/app-info
```

---

## 체크리스트

### 사전 준비
- [ ] 현재 브랜치 백업
- [ ] migration.v2.0.1.md 문서 작성

### Phase 1: 준비
- [ ] ProdcutApplication → ProductApplication 오타 수정
- [ ] 이동 대상 24개 컨트롤러 목록 확정

### Phase 2: 컨트롤러 이동
- [ ] Alcohols 도메인 (4개)
- [ ] User 도메인 (5개)
- [ ] Review 도메인 (3개)
- [ ] Support 도메인 (4개)
- [ ] External 도메인 (3개)
- [ ] 기타 도메인 (5개)

### Phase 3: 검증
- [ ] 컴파일 성공
- [ ] Import 경로 검증
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과

### Phase 4: 배포 검증
- [ ] JAR 빌드 성공
- [ ] Docker 이미지 빌드
- [ ] 로컬 실행 테스트
- [ ] Development 배포 테스트
- [ ] API 응답 검증

---

## 주의사항

### 1. Import 경로 유지
```java
// 올바른 import (DTO는 mono에서)
import app.bottlenote.user.dto.request.LoginRequest;
import app.bottlenote.user.dto.response.LoginResponse;
import app.bottlenote.user.service.AuthService;

// 잘못된 import
// 컨트롤러끼리는 서로 import 하지 않음
```

### 2. 패키지 구조 변경
- 기존: `app.bottlenote.{domain}.controller.*`
- 변경: `app.bottlenote.{domain}.presentation.*`
- external 도메인은 이미 presentation 사용

### 3. mono에 유지할 것
- DTO (request/response)
- Service/Facade
- Repository
- Exception 정의
- Converter
- GlobalExceptionHandler
- WebConfig

### 4. 순환 의존성 방지
- 컨트롤러 → DTO (mono) : 허용
- 컨트롤러 → Service (mono) : 허용
- 컨트롤러 ← DTO : 금지
- 컨트롤러 ← Service : 금지

### 5. 테스트 코드
- 컨트롤러 테스트는 product-api에 이미 존재
- 통합 테스트는 product-api에서 실행
- Service 테스트는 mono에 유지

---

## 완료 기준

### 기술적 완료
- [ ] 24개 컨트롤러 모두 product-api로 이동
- [ ] ProdcutApplication → ProductApplication 수정
- [ ] 모든 테스트 통과
- [ ] JAR 빌드 성공
- [ ] Docker 이미지 빌드 성공

### 배포 검증
- [ ] 로컬 실행 (포트 8080)
- [ ] Docker 실행 (포트 30001)
- [ ] Development 환경 배포
- [ ] API 엔드포인트 응답 확인
- [ ] Prometheus metrics 확인

### 문서화
- [ ] migration.v2.0.1.md 작성
- [ ] 이동 내역 정리
- [ ] 주의사항 문서화

---

## 마이그레이션 요약

### 이동 규모
- **컨트롤러**: 24개
- **라인 수**: 약 2,000줄 (추정)
- **예상 소요 시간**: 2-3시간

### 변경 파일
- **추가**: product-api 컨트롤러 24개
- **삭제**: mono 컨트롤러 24개
- **수정**: ProdcutApplication.java (오타 수정)

### 영향 범위
- **product-api**: 컨트롤러 추가
- **mono**: 컨트롤러 제거, 나머지 유지
- **배포**: 변경 없음 (기존 파이프라인 유지)

---

## 다음 단계 (v2.0.2 이후)

1. **Admin API 개발** (bottlenote-admin-api)
   - 관리자 전용 엔드포인트
   - 별도 포트 (30100)
   - Kotlin 기반

작성일: 2025-10-09
