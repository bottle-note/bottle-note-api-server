# bottlenote-observability 모듈 재구성 계획

## 목표
EMSP 프로젝트의 OpenTelemetry 통합 방식을 bottlenote에 적용
- **OpenTelemetry Collector** 중심 아키텍처 (grafana/docker-otel-lgtm)
- **단일 OTLP 엔드포인트**로 Logs/Traces/Metrics 통합 전송
- **OpenTelemetry Logback Appender** 사용 (Loki 직접 연동 제거)
- **AOP 자동 추적** 전체 적용 (Controller/Service/Repository)
- **단순화된 구현** (이중 구현체 제거)

---

## 참조 프로젝트

### EMSP 프로젝트
- **경로**: `/Users/hgkim/workspace/opnd/emsp/backend/`
- **브랜치**: `chore/grafana-lgtm-default-setting`
- **참조 내용**: TracingService, AOP, Config 구조

### LGTM 스택 (Grafana 공식)
- **레포지토리**: https://github.com/grafana/docker-otel-lgtm
- **참조 내용**: OpenTelemetry Collector 아키텍처, OTLP 엔드포인트 구조

---

## LGTM 스택 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│          bottlenote Spring Boot Application                 │
│                                                             │
│  단일 OTLP 엔드포인트로 모든 데이터 전송:                    │
│  - http://grafana.dead-whale.org:30318 (HTTP/Protobuf)     │
│  - Logs + Traces + Metrics 통합                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│       OpenTelemetry Collector (grafana/otel-lgtm)           │
│       NodePort: 30318 (HTTP), 30317 (gRPC)                  │
│                                                             │
│  자동 라우팅:                                                │
│  - Traces  → Tempo (4418)                                  │
│  - Logs    → Loki (3100/otlp)                              │
│  - Metrics → Prometheus (9090/api/v1/otlp)                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┬──────────────┐
        ▼                             ▼              ▼
   ┌────────┐                   ┌─────────┐    ┌──────────┐
   │ Tempo  │                   │  Loki   │    │Prometheus│
   │(Traces)│                   │ (Logs)  │    │(Metrics) │
   └────────┘                   └─────────┘    └──────────┘
        │                             │              │
        └─────────────────────────────┴──────────────┘
                       │
                       ▼
               ┌──────────────┐
               │  Grafana UI  │
               │  Port: 3000  │
               └──────────────┘
```

**핵심:**
- 애플리케이션은 **하나의 엔드포인트**만 설정
- Collector가 데이터 타입별로 자동 라우팅
- 벤더 중립적 (Loki → Datadog 등 변경 용이)

### 🔄 Loki 연동 방식 변경 (중요!)

**Loki는 계속 사용합니다!** 단지 전달 방식이 변경됩니다:

#### 변경 전: Loki 직접 연동
```
Application → loki-logback-appender → Loki HTTP API (직접)
```
- ❌ Loki 전용 (다른 백엔드로 변경 어려움)
- ❌ Traces와 수동으로 연결 필요
- ✅ 설정 단순

#### 변경 후: OTLP → Collector → Loki
```
Application → opentelemetry-logback-appender → OTLP Collector → Loki
```
- ✅ **Traces와 자동 연결** (trace ID, span ID 자동 포함)
- ✅ 벤더 중립적 (OpenTelemetry 표준)
- ✅ Logs/Traces/Metrics 통합 관측성
- ✅ 나중에 Datadog, Elastic 등으로 변경 용이

**최종 저장소는 동일: Loki**
- 로그는 여전히 Loki에 저장됨
- Grafana에서 Loki 데이터소스로 조회
- 차이점은 **전달 경로**만 변경

---

## 1단계: 의존성 수정

### gradle/libs.versions.toml 추가
```toml
[versions]
opentelemetry = "1.50.0"
opentelemetry-instrumentation = "2.10.0-alpha"
micrometer = "1.15.0"
micrometer-tracing = "1.5.0"

[libraries]
# OpenTelemetry (BOM 사용)
opentelemetry-bom = { module = "io.opentelemetry:opentelemetry-bom", version.ref = "opentelemetry" }
opentelemetry-api = { module = "io.opentelemetry:opentelemetry-api" }
opentelemetry-sdk = { module = "io.opentelemetry:opentelemetry-sdk" }
opentelemetry-exporter-otlp = { module = "io.opentelemetry:opentelemetry-exporter-otlp" }
opentelemetry-logback-appender = { module = "io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0", version.ref = "opentelemetry-instrumentation" }

# Micrometer
micrometer-core = { module = "io.micrometer:micrometer-core", version.ref = "micrometer" }
micrometer-tracing = { module = "io.micrometer:micrometer-tracing", version.ref = "micrometer-tracing" }
micrometer-tracing-bridge-otel = { module = "io.micrometer:micrometer-tracing-bridge-otel", version.ref = "micrometer-tracing" }
```

### bottlenote-observability/build.gradle 수정
```gradle
plugins {
    id 'java-library'
}

dependencies {
    // BOM (Bill of Materials)
    api platform(libs.opentelemetry.bom)

    // OpenTelemetry
    api libs.opentelemetry.api
    api libs.opentelemetry.sdk
    api libs.opentelemetry.exporter.otlp
    api libs.opentelemetry.logback.appender

    // Micrometer
    api libs.micrometer.core
    api libs.micrometer.tracing
    api libs.micrometer.tracing.bridge.otel

    // Spring Boot
    api libs.spring.boot.starter.actuator
    api libs.spring.boot.starter.aop

    // Lombok
    compileOnly libs.lombok
    annotationProcessor libs.lombok
}

bootJar {
    enabled = false
}

jar {
    enabled = true
}
```

**제거할 의존성:**
- ❌ `loki-logback-appender`

---

## 2단계: logback-spring.xml 수정

**파일**: `src/main/resources/logback-spring.xml`

**EMSP 방식 적용 (OpenTelemetry Appender):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
  <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

  <!-- OpenTelemetry Appender (OTLP로 로그 전송) -->
  <appender name="OpenTelemetry"
            class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender"/>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="OpenTelemetry"/>
  </root>

  <!-- 특정 라이브러리 로그 레벨 조정 -->
  <logger name="org.springframework.web" level="INFO"/>
  <logger name="org.hibernate" level="INFO"/>
  <logger name="io.opentelemetry" level="INFO"/>
</configuration>
```

**제거할 것:**
- ❌ Loki appender 설정
- ❌ 프로파일 분기 로직
- ❌ LOKI_DISABLED 환경변수 제어

---

## 3단계: application-observability.yml 수정

**파일**: `src/main/resources/application-observability.yml`

**단일 Collector 엔드포인트 방식 (권장):**
```yaml
spring:
  application:
    name: bottlenote

management:
  endpoints:
    web:
      exposure:
        include: [ "health", "info", "metrics", "prometheus", "tracing" ]
      base-path: /actuator
  endpoint:
    health:
      show-details: always

  # OpenTelemetry 트레이싱
  tracing:
    enabled: ${OTEL_SDK_DISABLED:true}  # 기본값 true (비활성화)
    sampling:
      probability: ${OTEL_TRACE_SAMPLING:0.3}

  # OTLP 설정 - 단일 엔드포인트로 통합
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://grafana.dead-whale.org:30318}
      timeout: ${OTEL_EXPORTER_OTLP_TIMEOUT:10s}
      protocol: ${OTEL_EXPORTER_OTLP_PROTOCOL:http/protobuf}
      compression: ${OTEL_EXPORTER_OTLP_COMPRESSION:gzip}

  # 메트릭스
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      slo:
        http.server.requests: 10ms, 50ms, 100ms, 200ms, 500ms
    export:
      otlp:
        enabled: ${OTEL_METRICS_ENABLED:false}
        url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://grafana.dead-whale.org:30318}
        step: 60s

  prometheus:
    metrics:
      export:
        enabled: ${PROMETHEUS_ENABLED:true}
```

**핵심 변경:**
- **단일 엔드포인트**: `OTEL_EXPORTER_OTLP_ENDPOINT` 하나로 통합
  - Logs: OpenTelemetry Appender가 자동으로 이 엔드포인트 사용
  - Traces: `management.otlp.tracing.endpoint`
  - Metrics: `management.metrics.export.otlp.url`
- **Collector가 자동 라우팅**:
  - `/v1/traces`, `/v1/logs` 같은 명시적 경로 불필요
  - Collector가 데이터 타입을 보고 Tempo/Loki/Prometheus로 자동 전달
- **기본값**: 트레이싱 비활성화 (로컬 개발 편의)

---

## 4단계: EMSP 코드 복사

### 4.1 TracingService 인터페이스

**원본**: `/Users/hgkim/workspace/opnd/emsp/backend/src/main/java/io/opnd/dev/git/emsp/backend/global/util/logger/TracingService.java`

**대상**: `src/main/java/app/bottlenote/observability/service/TracingService.java`

**수정사항:**
- 패키지명: `io.opnd.dev.git.emsp.backend.global.util.logger` → `app.bottlenote.observability.service`

---

### 4.2 MicrometerTracingService 구현체

**원본**: `/Users/hgkim/workspace/opnd/emsp/backend/src/main/java/io/opnd/dev/git/emsp/backend/global/util/logger/MicrometerTracingService.java`

**대상**: `src/main/java/app/bottlenote/observability/service/MicrometerTracingService.java`

**수정사항:**
- 패키지명 수정
- import 경로 수정

**주의:**
- `@ConditionalOnProperty` 유지: `management.tracing.enabled=false`일 때만 활성화

---

### 4.3 TracingConfiguration

**원본**: `/Users/hgkim/workspace/opnd/emsp/backend/src/main/java/io/opnd/dev/git/emsp/backend/global/config/TracingConfiguration.java`

**대상**: `src/main/java/app/bottlenote/observability/config/TracingConfiguration.java`

**수정사항:**
- 기존 `OpenTelemetryConfig.java` 대체
- 패키지명 수정
- OpenTelemetryAppender 초기화 로직 포함

**핵심 로직:**
```java
@Bean
public ApplicationListener<ApplicationReadyEvent> openTelemetryLogbackAppenderInitializer(
    OpenTelemetry openTelemetry) {
  return event -> {
    OpenTelemetryAppender.install(openTelemetry);
    log.info("OpenTelemetryAppender installed");
  };
}
```

---

### 4.4 MicrometerTracingAspect (AOP)

**원본**: `/Users/hgkim/workspace/opnd/emsp/backend/src/main/java/io/opnd/dev/git/emsp/backend/global/component/MicrometerTracingAspect.java`

**대상**: `src/main/java/app/bottlenote/observability/aop/MicrometerTracingAspect.java`

**수정사항:**
- 패키지명: `io.opnd.dev.git.emsp.backend.global.component` → `app.bottlenote.observability.aop`
- Pointcut 경로 수정:
  ```java
  // 변경 전
  @Around("execution(public * io.opnd.dev.git.emsp..service.*.*(..))")
  @Around("execution(public * io.opnd.dev.git.emsp..repository.*.*(..))")
  @Around("execution(public * io.opnd.dev.git.emsp..controller.*.*(..))")

  // 변경 후
  @Around("execution(public * app.bottlenote..service.*.*(..))")
  @Around("execution(public * app.bottlenote..repository.*.*(..))")
  @Around("execution(public * app.bottlenote..controller.*.*(..))")
  ```

**특이사항:**
- HealthCheckController 제외 로직 유지 (노이즈 방지)

---

### 4.5 @TraceMethod 어노테이션

**원본**: `/Users/hgkim/workspace/opnd/emsp/backend/src/main/java/io/opnd/dev/git/emsp/backend/global/annotation/TraceMethod.java`

**대상**: `src/main/java/app/bottlenote/observability/annotation/TraceMethod.java`

**수정사항:**
- 패키지명 수정

---

## 5단계: 디렉토리 구조 최종안

```
bottlenote-observability/
├── build.gradle (수정됨)
├── claude.plan.md (이 파일)
└── src/main/
    ├── java/app/bottlenote/observability/
    │   ├── annotation/
    │   │   └── TraceMethod.java (EMSP 복사)
    │   ├── aop/
    │   │   └── MicrometerTracingAspect.java (EMSP 복사 + 수정)
    │   ├── config/
    │   │   └── TracingConfiguration.java (EMSP 복사, 기존 OpenTelemetryConfig 대체)
    │   └── service/
    │       ├── TracingService.java (EMSP 복사)
    │       └── MicrometerTracingService.java (EMSP 복사)
    └── resources/
        ├── logback-spring.xml (수정됨 - OTLP 방식)
        └── application-observability.yml (수정됨 - OTLP Logs 추가)
```

**제거할 파일:**
- ❌ `config/OpenTelemetryConfig.java` (TracingConfiguration으로 대체)

---

## 6단계: 검증 방법

### 로컬 테스트 (기본값 - 비활성화)
```bash
# OTEL_SDK_DISABLED=true (기본값)
# OpenTelemetry 비활성화, 일반 로깅만 동작
./gradlew bootRun
```

### OTLP 전체 활성화 테스트
```bash
# Traces + Logs + Metrics 모두 Collector로 전송
OTEL_SDK_DISABLED=false \
OTEL_METRICS_ENABLED=true \
./gradlew bootRun
```

### Traces만 활성화 테스트
```bash
# Traces만 전송, Logs는 콘솔만
OTEL_SDK_DISABLED=false \
./gradlew bootRun
```

### Grafana에서 데이터 확인

1. **Grafana 접속**: http://grafana.dead-whale.org
   - Username: `admin`
   - Password: `admin`

2. **Explore 메뉴**에서 확인:
   - **Traces** (Tempo 데이터소스):
     - Service: `bottlenote` 선택
     - trace ID로 로그와 연결 확인
   - **Logs** (Loki 데이터소스):
     - Label: `{app="bottlenote"}`
     - trace ID 포함 여부 확인
   - **Metrics** (Prometheus 데이터소스):
     - `http_server_requests_seconds_count{service="bottlenote"}`

3. **통합 확인**:
   - Trace 상세에서 "Related logs" 클릭 → 자동으로 Loki 쿼리 연결
   - Log 상세에서 trace_id 클릭 → Tempo로 이동

---

## 주요 변경사항 요약

| 항목 | 기존 | 변경 후 |
|------|------|---------|
| 로그 전송 | Loki-logback-appender (직접) | OpenTelemetry Appender (OTLP) |
| 프로토콜 | HTTP (Loki 전용) | OTLP (http/protobuf + gzip) |
| 트레이싱 | 기본 설정 | TracingService + AOP 자동 추적 |
| 자동 추적 | 없음 | Controller/Service/Repository 전체 |
| 구현체 | 없음 | MicrometerTracingService |
| 제어 방식 | 개별 환경변수 | OTEL_SDK_DISABLED 통합 |

---

## 환경변수 목록

| 환경변수 | 기본값 | 설명 |
|---------|-------|------|
| `OTEL_SDK_DISABLED` | `true` | OpenTelemetry 활성화/비활성화 |
| `OTEL_TRACE_SAMPLING` | `0.3` | 트레이스 샘플링 비율 (30%) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://grafana.dead-whale.org:30318` | **단일 OTLP Collector 엔드포인트** |
| `OTEL_EXPORTER_OTLP_TIMEOUT` | `10s` | OTLP 요청 타임아웃 |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `http/protobuf` | OTLP 프로토콜 (gRPC 사용 시 `grpc`) |
| `OTEL_EXPORTER_OTLP_COMPRESSION` | `gzip` | 압축 방식 (gzip, none) |
| `OTEL_METRICS_ENABLED` | `false` | OTLP 메트릭 전송 활성화 |
| `PROMETHEUS_ENABLED` | `true` | Prometheus 메트릭 엔드포인트 활성화 |

**주요 포인트:**
- `OTEL_EXPORTER_OTLP_ENDPOINT`: **하나의 엔드포인트**로 Logs/Traces/Metrics 모두 전송
- Collector가 내부적으로 Loki/Tempo/Prometheus로 라우팅
- gRPC 사용 시: `http://grafana.dead-whale.org:30317`, 프로토콜 `grpc`

---

## 참고 문서

### 프로젝트 문서
- **EMSP 프로젝트**: `/Users/hgkim/workspace/opnd/emsp/backend/`
  - TracingService, AOP, Config 구조 참조
- **LGTM 스택 배포 문서**: `/Users/hgkim/workspace/bottlenote/bottle-note-api-server/git.environment-variables/deploy/monitoring/README.md`
  - bottlenote의 LGTM 스택 배포 정보

### 공식 문서
- **grafana/docker-otel-lgtm**: https://github.com/grafana/docker-otel-lgtm
  - LGTM 스택의 공식 레퍼런스 구현
  - Collector 설정 파일: `docker/otelcol-config.yaml`
  - Spring Boot 연동 예제: `examples/java/`
- **OpenTelemetry 공식**: https://opentelemetry.io/docs/
  - OTLP 프로토콜 스펙
  - Instrumentation 가이드
- **Micrometer 문서**: https://micrometer.io/docs/tracing
  - Spring Boot 트레이싱 통합
  - Bridge 설정 방법

---

## 작업 순서

1. ✅ 계획 수립 (이 파일)
2. ✅ gradle/libs.versions.toml 수정
3. ✅ bottlenote-observability/build.gradle 수정
4. ✅ logback-spring.xml 수정
5. ✅ application-observability.yml 수정
6. ✅ EMSP 코드 복사 (TracingService, Config, AOP, Annotation)
7. ✅ 패키지명 및 Pointcut 경로 수정
8. ✅ 기존 OpenTelemetryConfig.java 제거
9. ⬜ 빌드 테스트 (선택적)
10. ⬜ 로컬 실행 테스트