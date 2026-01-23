# Spring Batch 테스트 가이드

## 목차
1. [현재 프로젝트 테스트 현황](#1-현재-프로젝트-테스트-현황)
2. [테스트 유형별 접근법](#2-테스트-유형별-접근법)
3. [핵심 테스트 도구](#3-핵심-테스트-도구)
4. [프로젝트 Job별 테스트 전략](#4-프로젝트-job별-테스트-전략)
5. [테스트 코드 예시](#5-테스트-코드-예시)
6. [JDBC vs InMemoryRepository](#6-jdbc-vs-inmemoryrepository)
7. [권장 사항](#7-권장-사항)

---

## 1. 현재 프로젝트 테스트 현황

| 파일 | 상태 | 설명 |
|------|------|------|
| `BatchConfigTest.java` | 비어있음 | 구현 필요 |
| `DailyDataReportQuartzJobTest.java` | 구현됨 | Quartz Job 단위 테스트 (Mock 기반) |

현재는 Quartz Job의 호출 여부만 검증하고, 실제 Batch Job 로직 테스트는 없는 상태입니다.

### 배치 모듈 구조

```
app.batch.bottlenote/
├── config/
│   └── QuartzConfig.java              # Quartz 스케줄러 설정
├── properties/
│   └── PopularAlcoholProperties.java  # 인기 주류 설정
└── job/
    ├── ranking/
    │   ├── BestReviewSelectionJobConfig.java     # Chunk 기반 (다중 Step)
    │   └── PopularAlcoholSelectionJobConfig.java # Chunk 기반
    └── report/
        └── DailyDataReportJobConfig.java         # Tasklet 기반
```

---

## 2. 테스트 유형별 접근법

### 2.1 단위 테스트 (Unit Test)

**목표**: Reader, Processor, Writer 개별 비즈니스 로직 검증

**특징**:
- 외부 의존성 Mock 처리
- 빠른 실행 속도
- `@Tag("unit")` 사용

```java
@Tag("unit")
@SpringBatchTest
@SpringJUnitConfig(classes = {BatchConfig.class})
class MyItemReaderTest {
    @Autowired
    private ItemReader<Data> itemReader;

    // StepScope 빈의 JobParameters 주입을 위한 팩토리 메서드
    public StepExecution getStepExecution() {
        return MetaDataInstanceFactory.createStepExecution();
    }

    @Test
    @DisplayName("ItemReader가 데이터를 정상적으로 읽는다")
    void testRead() {
        // when
        Data data = itemReader.read();

        // then
        assertThat(data).isNotNull();
    }
}
```

### 2.2 통합 테스트 (Integration Test)

**목표**: End-to-End Job 실행 및 DB 상태 변화 검증

**특징**:
- TestContainers로 실제 DB 환경 구성
- 전체 Job 흐름 검증
- `@Tag("integration")` 사용

```java
@Tag("integration")
@SpringBatchTest
@SpringBootTest
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = NONE)
class BatchJobIntegrationTest {
    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private TestRepository testRepository;

    @Test
    @DisplayName("Job이 정상적으로 완료된다")
    void testCompleteJobExecution() {
        // given: 테스트 데이터 준비
        testRepository.save(testData);

        // when: Job 실행
        JobExecution execution = jobLauncherTestUtils.launchJob();

        // then: 결과 검증
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(testRepository.findAll()).hasSize(expectedSize);
    }
}
```

### 2.3 슬라이스 테스트 (Slice Test)

**목표**: 특정 Step만 격리하여 테스트

**특징**:
- 다중 Step Job에서 특정 Step만 검증
- 이전 Step 영향 없이 독립 테스트

```java
@Test
@DisplayName("베스트 리뷰 초기화 Step이 정상 동작한다")
void testResetBestReviewStep() {
    // when: 특정 Step만 실행
    JobExecution execution = jobLauncherTestUtils.launchStep("resetBestReviewStep");

    // then
    assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
}
```

---

## 3. 핵심 테스트 도구

### 3.1 @SpringBatchTest

Spring Batch 테스트를 위한 통합 설정 어노테이션

**자동 제공 기능**:
- `JobLauncherTestUtils` 빈 자동 등록
- `JobRepositoryTestUtils` 빈 자동 등록
- `StepScopeTestExecutionListener` 자동 추가
- `JobScopeTestExecutionListener` 자동 추가

### 3.2 JobLauncherTestUtils

| 메서드 | 설명 |
|--------|------|
| `launchJob(JobParameters)` | 전체 Job 실행 |
| `launchStep(String stepName)` | 특정 Step만 실행 |
| `setJob(Job job)` | 테스트할 Job 설정 |

### 3.3 StepScopeTestExecutionListener

`@StepScope` 빈의 JobParameters 주입을 위한 리스너

**사용 방법**:
```java
// 팩토리 메서드 정의 (리스너가 자동 감지)
public StepExecution getStepExecution() {
    StepExecution execution = MetaDataInstanceFactory.createStepExecution();
    // JobParameters 추가 가능
    execution.getJobExecution().getJobParameters()
        .addString("input.file", "test.csv");
    return execution;
}
```

### 3.4 MetaDataInstanceFactory

테스트용 메타데이터 객체 생성 유틸리티

| 메서드 | 설명 |
|--------|------|
| `createStepExecution()` | 기본 StepExecution 생성 |
| `createStepExecution(String, Long)` | 이름과 ID로 생성 |
| `createJobExecution()` | JobExecution 생성 |

---

## 4. 프로젝트 Job별 테스트 전략

### 4.1 DailyDataReportJobConfig (Tasklet 기반)

**특성**: 단순 작업 (데이터 수집 -> Discord 웹훅 전송)

**테스트 전략**:
- Mock 서비스 + 단위 테스트
- `DailyDataReportService` Mock 처리
- 웹훅 호출 여부 검증

```java
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DailyDataReportJobConfigTest {
    @Mock
    private DailyDataReportService dailyDataReportService;

    @Mock
    private DiscordWebhookProperties discordWebhookProperties;

    @Test
    @DisplayName("일일 리포트 Job이 서비스를 정상 호출한다")
    void testDailyDataReportJob() {
        // given
        when(discordWebhookProperties.getUrl()).thenReturn("https://webhook.url");

        // when: Job 실행
        // ...

        // then
        verify(dailyDataReportService, times(1))
            .collectAndSendDailyReport(any(LocalDate.class), anyString());
    }
}
```

### 4.2 PopularAlcoholSelectionJobConfig (Chunk 기반)

**특성**: 복잡한 처리 (ItemReader -> Processor -> ItemWriter)

**테스트 전략**:
- TestContainers + 통합 테스트
- 실제 DB 데이터 기반 검증
- 청크 처리 결과 검증

```java
@Tag("integration")
@SpringBatchTest
@SpringBootTest
@Import(TestContainersConfig.class)
class PopularAlcoholSelectionJobIntegrationTest {
    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        jdbcTemplate.execute("INSERT INTO alcohols ...");
    }

    @Test
    @DisplayName("인기 주류 선정 Job이 정상 완료된다")
    void testPopularAlcoholJob() {
        // when
        JobExecution execution = jobLauncherTestUtils.launchJob();

        // then
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM popular_alcohols WHERE year = ? AND month = ? AND day = ?",
            Integer.class, LocalDate.now().getYear(), LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth()
        );
        assertThat(count).isGreaterThan(0);
    }
}
```

### 4.3 BestReviewSelectionJobConfig (다중 Step Chunk 기반)

**특성**: 다중 Step (초기화 Step -> 선정 Step)

**테스트 전략**:
- Step 슬라이스 테스트 + 통합 테스트
- 각 Step 독립 검증
- 전체 Job 흐름 검증

```java
@Tag("integration")
@SpringBatchTest
@SpringBootTest
@Import(TestContainersConfig.class)
class BestReviewSelectionJobIntegrationTest {
    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    @DisplayName("베스트 리뷰 초기화 Step이 모든 리뷰를 초기화한다")
    void testResetStep() {
        // given: 베스트 리뷰 데이터 준비

        // when
        JobExecution execution = jobLauncherTestUtils.launchStep("resetBestReviewStep");

        // then
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        // 모든 is_best가 false인지 검증
    }

    @Test
    @DisplayName("베스트 리뷰 선정 Step이 올바른 리뷰를 선정한다")
    void testSelectionStep() {
        // given: 리뷰 데이터 준비

        // when
        JobExecution execution = jobLauncherTestUtils.launchStep("bestReviewSelectedStep");

        // then
        assertThat(execution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        // 선정된 리뷰 검증
    }
}
```

---

## 5. 테스트 코드 예시

### 5.1 Quartz Job 테스트 (현재 구현됨)

```java
@Tag("unit")
@DisplayName("[unit] [schedule] DailyDataReportQuartzJob")
@ExtendWith(MockitoExtension.class)
class DailyDataReportQuartzJobTest {

    @Mock private JobLauncher jobLauncher;
    @Mock private JobRegistry jobRegistry;
    @Mock private JobExecutionContext context;
    @Mock private Job job;

    private DailyDataReportQuartzJob dailyDataReportQuartzJob;
    private AppInfoConfig appInfoConfig;

    @BeforeEach
    void setUp() {
        appInfoConfig = new AppInfoConfig();
        appInfoConfig.setEnvironment("prod");
        dailyDataReportQuartzJob = new DailyDataReportQuartzJob(jobLauncher, jobRegistry, appInfoConfig);
    }

    @Test
    @DisplayName("prod 환경에서는 Job이 정상 실행된다")
    void testExecuteInternal_prod환경실행() throws Exception {
        // given
        when(jobRegistry.getJob(eq(DAILY_DATA_REPORT_JOB_NAME))).thenReturn(job);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(null);

        // when
        dailyDataReportQuartzJob.executeInternal(context);

        // then
        verify(jobRegistry, times(1)).getJob(DAILY_DATA_REPORT_JOB_NAME);
        verify(jobLauncher, times(1)).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("dev 환경에서는 Job이 실행되지 않는다")
    void testExecuteInternal_dev환경스킵() throws Exception {
        // given
        appInfoConfig.setEnvironment("dev");
        dailyDataReportQuartzJob = new DailyDataReportQuartzJob(jobLauncher, jobRegistry, appInfoConfig);

        // when
        dailyDataReportQuartzJob.executeInternal(context);

        // then
        verify(jobRegistry, times(0)).getJob(any());
        verify(jobLauncher, times(0)).run(any(Job.class), any(JobParameters.class));
    }
}
```

### 5.2 Batch Job 통합 테스트 템플릿

```java
@Tag("integration")
@DisplayName("[integration] [batch] PopularAlcoholSelectionJob")
@SpringBatchTest
@SpringBootTest
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PopularAlcoholSelectionJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job popularAlcoholJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(popularAlcoholJob);
        // 테스트 데이터 초기화
        jdbcTemplate.execute("DELETE FROM popular_alcohols");
    }

    @Test
    @DisplayName("인기 주류 선정 Job이 정상적으로 완료된다")
    void 인기_주류_선정_Job이_정상적으로_완료된다() throws Exception {
        // given: 테스트 데이터 준비
        // ...

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("인기 주류 데이터가 정상적으로 저장된다")
    void 인기_주류_데이터가_정상적으로_저장된다() throws Exception {
        // given
        LocalDate today = LocalDate.now();

        // when
        jobLauncherTestUtils.launchJob();

        // then
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM popular_alcohols WHERE year = ? AND month = ? AND day = ?",
            Integer.class,
            today.getYear(),
            today.getMonthValue(),
            today.getDayOfMonth()
        );
        assertThat(count).isGreaterThan(0);
    }
}
```

---

## 6. JDBC vs InMemoryRepository

### 6.1 Spring Batch가 JDBC를 사용하는 이유

**JobRepository 메타데이터 저장**:
- `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION` 등 테이블
- Job 실행 상태, 재시작 지점, 청크 커밋 추적
- 실패 시 어디서부터 재시작할지 기록

이것은 **배치 프레임워크 인프라** 요구사항이다.

### 6.2 비즈니스 로직에서 JdbcTemplate 직접 사용 이유

**대량 데이터 처리 성능**:
| 항목 | JPA/Repository | JdbcTemplate |
|------|----------------|--------------|
| 벌크 연산 | 엔티티 하나씩 로드 후 변경 | 한 방 쿼리 |
| 메모리 | 1차 캐시에 전부 로드 (OOM 위험) | 필요한 것만 처리 |
| 변경 감지 | Hibernate dirty checking 오버헤드 | 없음 |

### 6.3 테스트에서의 선택

**프로덕션**: 대량 처리 → JdbcTemplate 유리

**테스트**: 데이터 몇 건 → **InMemoryRepository로 충분**

현재 `BestReviewSelectionJobConfig`가 JdbcTemplate을 직접 사용하는 이유는 ReviewRepository에 배치용 메서드가 없기 때문. 필요한 메서드를 추가하면 InMemoryRepository로 테스트 가능:
- `resetAllBestReviews()` - 모든 is_best 초기화
- `findBestReviewCandidates()` - 베스트 후보 조회
- `updateBestReviews(List<Long> ids)` - 베스트 업데이트

---

## 7. 권장 사항

### 7.1 테스트 분리 원칙

| 구분 | Quartz Job | Spring Batch Job |
|------|------------|------------------|
| 역할 | 언제/어떻게 실행 | 무엇을 실행 |
| 테스트 방식 | Mock 기반 단위 테스트 | 통합 테스트 / 슬라이스 테스트 |
| 검증 포인트 | JobLauncher 호출 여부 | 실제 데이터 처리 결과 |

### 7.2 프로젝트 패턴 적용

1. **InMemory Repository 패턴**: DB 의존성 제거한 단위 테스트
2. **TestFactory 패턴**: 테스트 데이터 빌더
3. **Given-When-Then 구조**: 명확한 테스트 구조
4. **테스트 태그 구분**: `@Tag("unit")`, `@Tag("integration")`

### 7.3 테스트 실행 명령어

```bash
# 배치 모듈 테스트 (@Tag("batch"))
./gradlew :bottlenote-batch:batch_test

# 특정 테스트 클래스 실행
./gradlew :bottlenote-batch:batch_test --tests "BestReviewSelectionJobConfigTest"

# 루트에서 전체 배치 테스트
./gradlew batch_test
```

### 7.4 CI/CD 고려사항

- 배치 테스트는 실행 시간이 길 수 있으므로 별도 파이프라인 고려
- 통합 테스트는 TestContainers 필요 (Docker 환경)
- 단위 테스트와 통합 테스트를 분리하여 실행

---

## 참고 자료

- [Spring Batch Testing Reference](https://docs.spring.io/spring-batch/reference/testing.html)
- [Testing a Spring Batch Job | Baeldung](https://www.baeldung.com/spring-batch-testing-job)
- [JobLauncherTestUtils API](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/test/JobLauncherTestUtils.html)
- [StepScopeTestExecutionListener API](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/test/StepScopeTestExecutionListener.html)
- [Spring Batch - Tasklets vs Chunks](https://www.baeldung.com/spring-batch-tasklet-chunk)
