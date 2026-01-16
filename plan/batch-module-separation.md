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
┌─────────────────────────────────────┐     ┌─────────────────────────────────────┐
│   bottlenote-product-api            │     │   bottlenote-batch-app              │
│   (REST API 서버)                    │     │   (배치 전용 데몬)                   │
├─────────────────────────────────────┤     ├─────────────────────────────────────┤
│  - HTTP API 제공                     │     │  - Quartz 스케줄러                   │
│  - 톰캣 내장                          │     │  - Spring Batch Jobs                │
│  - batch 의존성 제거                  │     │  - 톰캣 없음 (web-application: none) │
└────────────────────┬────────────────┘     └────────────────────┬────────────────┘
                     │                                           │
                     └────────────────┬──────────────────────────┘
                                      │
                     ┌────────────────▼────────────────┐
                     │      bottlenote-mono            │
                     │      (공유 라이브러리)           │
                     └─────────────────────────────────┘
```

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
# application-batch.yml 추가 설정
spring:
  main:
    web-application-type: none  # 톰캣 비활성화
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

## 미결정 사항

1. **워크플로우 전략**: 기존 워크플로우에 batch 추가 vs 별도 워크플로우 생성
2. **배포 방식**: Deployment (상주형) vs CronJob (실행형) - 현재 Quartz 사용 중이므로 Deployment 권장
3. **리소스 할당**: batch 서버 CPU/메모리 스펙
