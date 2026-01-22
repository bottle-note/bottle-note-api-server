```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **IN PROGRESS**
Start Date: 2024-11-21
Last Updated: 2026-01-22

** Completed Work **
- Product-API에서 batch 의존성 제거
- BatchApplication.java 진입점 추가 (독립 실행 가능)
- Dockerfile-batch 생성
- K3s 배포 리소스 추가 (batch-module.yaml, patch, secret)
- BestReviewReader 무한 루프 버그 수정

** Key Components **
- BatchApplication.java: 배치 모듈 진입점
- Dockerfile-batch: 배치 컨테이너 이미지 빌드
- git.environment-variables/deploy/base/batch-module.yaml: K3s Deployment

** Remaining Work **
- GitHub Actions 워크플로우 추가 (batch 빌드/배포 자동화)
- 로컬/개발/운영 환경 배포 테스트
================================================================================
```

# Batch 모듈 분리 계획

## 개요

현재 `bottlenote-batch` 모듈이 `bottlenote-product-api`에 의존성으로 포함되어 함께 기동되고 있다.
이를 독립적인 애플리케이션으로 분리하여 별도 프로세스로 운영하는 것을 목표로 한다.

## 현재 구조

```
bottlenote-product-api (실행 가능 JAR)
├── bottlenote-mono (공유 라이브러리)
├── bottlenote-observability
└── bottlenote-batch (Quartz + Spring Batch)  ← 분리 대상
```

### 현재 의존성 (bottlenote-product-api/build.gradle:20)

```gradle
implementation project(':bottlenote-batch')
```

### 현재 설정 연결 (application.yml)

```yaml
spring:
  profiles:
    include:
      - batch  # batch 프로파일 활성화
```

## 분리 목표 구조

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                           배포 모듈 (실행 가능 JAR)                             │
├───────────────────────┬───────────────────────┬───────────────────────────────┤
│   product-api         │   admin-api           │   batch-app                   │
│   :8080               │   :8081               │   :8082                       │
│   (REST API)          │   (관리자 API)         │   (Quartz + Batch)            │
└───────────┬───────────┴───────────┬───────────┴───────────────┬───────────────┘
            │                       │                           │
            └───────────────────────┼───────────────────────────┘
                                    │
            ┌───────────────────────▼───────────────────────┐
            │              공유 라이브러리 (JAR)              │
            ├───────────────────────┬───────────────────────┤
            │   bottlenote-mono     │   bottlenote-          │
            │   (도메인/비즈니스)    │   observability        │
            └───────────────────────┴───────────────────────┘
```

### 설계 원칙

- **mono 모듈**: 거대한 공유 라이브러리로 유지 (web 의존성 포함)
- **배포 모듈**: 각각 독립 실행 가능한 Spring Boot JAR
- **헬스체크**: mono가 web 의존성을 가지므로 톰캣 활용, 포트만 분리

## 분리 작업 항목

### 1. Product-API 변경

| 파일 | 변경 내용 |
|------|----------|
| `bottlenote-product-api/build.gradle` | `implementation project(':bottlenote-batch')` 제거 |
| `application.yml` | `profiles.include`에서 `batch` 제거 |

### 2. Batch 모듈 변경

| 파일 | 변경 내용 |
|------|----------|
| `bottlenote-batch/build.gradle` | bootJar 활성화, 실행 가능 JAR 설정 |
| `BatchApplication.java` (신규) | `@SpringBootApplication` 진입점 추가 |
| `application-batch.yml` | `web-application-type: none` 설정 |

### 3. 배치 모듈 신규 설정

```yaml
# application.yml (batch 전용 - profile include 없이 독립 사용)
server:
  port: 8082

spring:
  application:
    name: bottlenote-batch

  # Batch 설정
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: never

  # Quartz 설정
  quartz:
    job-store-type: jdbc
    # ... 기존 quartz 설정 유지

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
```

```java
// BatchApplication.java (신규)
@SpringBootApplication
public class BatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
```

## 배포 관련 작업

### 1. K3s 배포 리소스 추가 (git.environment-variables)

서브모듈 `git.environment-variables/deploy` 경로에 배치 서버 배포 리소스 추가 필요.

| 경로 | 설명 |
|------|------|
| `deploy/base/batch-app.yaml` | Deployment 정의 (CronJob 또는 Deployment) |
| `deploy/overlays/development/batch-app-patch.yaml` | dev 환경 패치 |
| `deploy/overlays/production/batch-app-patch.yaml` | prod 환경 패치 |
| `deploy/base/kustomization.yaml` | batch-app.yaml 리소스 추가 |

### 2. GitHub Actions 워크플로우 추가

기존 워크플로우 참조: `.github/workflows/deploy_v2_development.yml`

신규 워크플로우 생성 필요: `deploy_v2_batch.yml` (예상)

| 작업 | 설명 |
|------|------|
| JAR 빌드 | `./gradlew :bottlenote-batch:build` |
| Docker 이미지 빌드 | `Dockerfile-batch` 신규 생성 |
| 이미지 푸시 | 레지스트리에 batch 이미지 푸시 |
| Kustomize 업데이트 | batch 이미지 태그 업데이트 |

### 3. 기존 워크플로우 구조 참고

현재 `deploy_v2_development.yml` 구조:
- `prepare-build`: JAR 빌드 및 artifact 업로드
- `build-product-image`: Docker 이미지 빌드/푸시
- `build-admin-image`: Docker 이미지 빌드/푸시
- `update-development`: Kustomize 이미지 태그 업데이트

**고려 사항**: 기존 워크플로우가 product/admin을 함께 빌드하는 구조인데, batch를 추가할지 별도 워크플로우로 분리할지 결정 필요.

## 현재 배치 작업 목록

| Job 이름 | Cron 표현식 | 역할 |
|----------|------------|------|
| bestReviewSelectedJob | 0 0 0 * * ? (매일 자정) | 베스트 리뷰 선정 |
| popularAlcoholJob | 0 0 0 * * ? (매일 자정) | 인기 위스키 선정 |
| dailyDataReportJob | 0 0 10 * * ? (매일 10시) | 일일 데이터 리포트 (Discord) |

## 의존성 분석 결과

### Product-API에서 Batch 직접 참조

- **코드 레벨**: 없음 (import 없음)
- **설정 레벨**: `application.yml`에서 batch 프로파일 include
- **빈 레벨**: ComponentScan으로 Quartz Job들 자동 등록

### 분리 영향도

| 항목 | 영향도 | 설명 |
|------|--------|------|
| API 기능 | 없음 | API 코드가 batch를 참조하지 않음 |
| 스케줄 작업 | 높음 | 분리 후 batch 별도 기동 필요 |
| 테스트 | 중간 | DailyDataReportService 관련 테스트 확인 필요 |

## 체크리스트

- [ ] Product-API에서 batch 의존성 제거
- [ ] Batch 모듈 독립 실행 가능하게 변경
- [ ] Dockerfile-batch 생성
- [ ] K3s 배포 리소스 추가 (서브모듈)
- [ ] GitHub Actions 워크플로우 추가/수정
- [ ] 로컬 테스트 (batch 단독 기동)
- [ ] 개발 환경 배포 테스트
- [ ] 운영 환경 배포

## 결정 사항

| 항목 | 결정 | 이유 |
|------|------|------|
| 헬스체크 | 톰캣 사용 (포트 8082) | mono가 web 의존성 포함, actuator `/health` 활용 |
| 배포 방식 | Deployment (상주형) | Quartz 내장 스케줄러 사용 |
| 설정 방식 | 독립 `application.yml` | profile include 없이 배치 전용 설정 |

## DB 커넥션 풀 설계

### 배경

- 프리티어 DB 사용 중 (max_connections: 60~100개 추정)
- 3개 앱이 각각 커넥션 풀을 가지므로 총량 조정 필요

### 권장 배분

| 앱 | 풀 사이즈 | 이유 |
|---|---|---|
| product-api | 10~15 | 메인 트래픽 처리, 동시 요청 많음 |
| admin-api | 5 | 관리자 전용, 사용 빈도 낮음 |
| batch-app | 3 | 스케줄 작업, 동시성 낮음 |
| **여유분** | 나머지 | 시스템/모니터링 용도 |

### batch-app 설정 예시

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 3
      minimum-idle: 1
      connection-timeout: 30000
      idle-timeout: 600000
```

### 고려사항

- 배치는 동시 요청이 거의 없음 (스케줄 기반)
- 매일 자정에 Job 3개 순차 실행 → 커넥션 2~3개면 충분
- product-api가 주력이므로 커넥션 여유 확보 필요

## 미결정 사항

1. **워크플로우 전략**: 기존 워크플로우에 batch 추가 vs 별도 워크플로우 생성
2. **리소스 할당**: batch 서버 CPU/메모리 스펙

---

## 알려진 버그: BestReviewReader 무한 루프

> 분리 작업 전 반드시 수정 필요

### 현상

- `bestReviewSelectedStep`이 종료되지 않고 `STARTED` 상태로 유지
- READ_COUNT/WRITE_COUNT가 비정상적으로 증가 (수천만~수억 건)
- 베스트 리뷰 초기화(`resetBestReviewStep`)는 완료되지만, 새로운 베스트 리뷰 선정이 끝나지 않음

### 증거 (BATCH_STEP_EXECUTION 테이블)

```
STEP_NAME              | STATUS  | READ_COUNT  | WRITE_COUNT
-----------------------|---------|-------------|-------------
bestReviewSelectedStep | STARTED | 111,320,100 | 111,320,100  ← 무한 루프
resetBestReviewStep    | COMPLETED | 0         | 0            ← 정상
```

### 원인

`BestReviewReader.read()` 메서드의 논리 오류:

```java
@Override
public BestReviewPayload read() {
    if (results == null) {
        results = jdbcTemplate.query(query, ...);  // 쿼리 실행
        currentIndex = 0;
    }

    BestReviewPayload nextItem = null;
    if (currentIndex < results.size()) {
        nextItem = results.get(currentIndex);
        currentIndex++;
    }

    if (currentIndex >= results.size()) {
        results = null;  // ← 문제: 마지막 아이템 반환 시 null로 초기화
    }

    return nextItem;  // ← 마지막 아이템 반환
}
```

**문제 흐름** (size=100 가정):

1. `currentIndex = 99` (마지막)
2. `nextItem = results.get(99)` 할당
3. `currentIndex++` → 100
4. `100 >= 100` → `results = null`
5. `return nextItem` (마지막 아이템 반환)
6. 다음 `read()` 호출 → `results == null` → 쿼리 다시 실행 → **무한 반복**

**올바른 동작**: 마지막 아이템 반환 후, 다음 `read()` 호출 시 `null`을 반환해야 배치가 종료됨.

### 해결 방안

```java
@Override
public BestReviewPayload read() {
    if (results == null) {
        results = jdbcTemplate.query(query, new BestReviewPayload.BestReviewMapper());
        currentIndex = 0;
    }

    if (currentIndex >= results.size()) {
        return null;  // 모든 아이템 처리 완료 → 배치 종료
    }

    return results.get(currentIndex++);
}
```

### 파일 위치

`bottlenote-batch/src/main/java/app/batch/bottlenote/job/BestReviewSelectionJobConfig.java` (132~162 라인)
