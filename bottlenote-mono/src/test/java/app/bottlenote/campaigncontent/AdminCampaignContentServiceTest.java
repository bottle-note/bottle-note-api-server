package app.bottlenote.campaigncontent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import app.bottlenote.campaigncontent.constant.CampaignContentEventType;
import app.bottlenote.campaigncontent.domain.CampaignContent;
import app.bottlenote.campaigncontent.domain.CampaignContentEventCounts;
import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentCreateRequest;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentMetricsRequest;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentSearchRequest;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentUpdateRequest;
import app.bottlenote.campaigncontent.dto.response.AdminCampaignContentListResponse;
import app.bottlenote.campaigncontent.dto.response.AdminCampaignContentMetricsResponse;
import app.bottlenote.campaigncontent.exception.CampaignContentException;
import app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode;
import app.bottlenote.campaigncontent.fixture.InMemoryCampaignContentEventRepository;
import app.bottlenote.campaigncontent.fixture.InMemoryCampaignContentMetricsRepository;
import app.bottlenote.campaigncontent.fixture.InMemoryCampaignContentRepository;
import app.bottlenote.campaigncontent.service.AdminCampaignContentService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.statistics.facade.payload.VisitorExclusionItem;
import app.bottlenote.statistics.fixture.FakeVisitorStatisticsFacade;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
@DisplayName("캠페인 콘텐츠 어드민 서비스")
class AdminCampaignContentServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  private static final Instant NOW = Instant.parse("2026-09-15T06:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZONE);
  private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);

  private InMemoryCampaignContentRepository campaignContentRepository;
  private InMemoryCampaignContentEventRepository eventRepository;
  private InMemoryCampaignContentMetricsRepository metricsRepository;
  private FakeVisitorStatisticsFacade visitorStatisticsFacade;
  private AdminCampaignContentService service;

  @BeforeEach
  void setUp() {
    campaignContentRepository = new InMemoryCampaignContentRepository();
    eventRepository = new InMemoryCampaignContentEventRepository(campaignContentRepository);
    metricsRepository = new InMemoryCampaignContentMetricsRepository();
    visitorStatisticsFacade = new FakeVisitorStatisticsFacade();
    service =
        new AdminCampaignContentService(
            campaignContentRepository,
            eventRepository,
            metricsRepository,
            visitorStatisticsFacade,
            FIXED_CLOCK);
  }

  @Test
  @DisplayName("형식에 맞는 코드로 등록할 때 활성 상태로 저장한다")
  void 캠페인_콘텐츠를_등록할_수_있다() {
    AdminResultResponse result =
        service.create(
            new AdminCampaignContentCreateRequest("whiskey-mbti", "위스키 MBTI", "설명", null));

    CampaignContent saved = campaignContentRepository.findById(result.targetId()).orElseThrow();
    assertThat(saved.getCode()).isEqualTo("whiskey-mbti");
    assertThat(saved.getIsActive()).isTrue();
    assertThat(result.code()).isEqualTo("CAMPAIGN_CONTENT_CREATED");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"Whiskey", "whiskey_mbti", "-mbti", "mbti-", "whiskey--mbti", "위스키"})
  @DisplayName("코드 형식이 맞지 않을 때 INVALID_CODE로 거절한다")
  void 형식이_맞지_않는_코드는_거절한다(String code) {
    assertThatThrownBy(
            () -> service.create(new AdminCampaignContentCreateRequest(code, "이름", null, true)))
        .isInstanceOf(CampaignContentException.class)
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_INVALID_CODE);
  }

  @Test
  @DisplayName("코드가 50자를 넘을 때 INVALID_CODE로 거절한다")
  void 코드가_50자를_넘으면_거절한다() {
    String code = "a".repeat(51);

    assertThatThrownBy(
            () -> service.create(new AdminCampaignContentCreateRequest(code, "이름", null, true)))
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_INVALID_CODE);
  }

  @Test
  @DisplayName("이미 쓰는 코드로 등록할 때 DUPLICATE_CODE로 거절한다")
  void 중복_코드는_거절한다() {
    persist("whiskey-mbti", "위스키 MBTI", true);

    assertThatThrownBy(
            () ->
                service.create(
                    new AdminCampaignContentCreateRequest("whiskey-mbti", "다른 이름", null, true)))
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_DUPLICATE_CODE);
  }

  @Test
  @DisplayName("수정할 때 이름·설명·활성 여부만 바뀌고 코드는 유지한다")
  void 수정해도_코드는_바뀌지_않는다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);

    service.update(content.getId(), new AdminCampaignContentUpdateRequest("새 이름", "새 설명", false));

    CampaignContent updated = campaignContentRepository.findById(content.getId()).orElseThrow();
    assertThat(updated.getCode()).isEqualTo("whiskey-mbti");
    assertThat(updated.getName()).isEqualTo("새 이름");
    assertThat(updated.getDescription()).isEqualTo("새 설명");
    assertThat(updated.getIsActive()).isFalse();
  }

  @Test
  @DisplayName("참여 기록이 있는 캠페인 콘텐츠를 삭제할 때 HAS_EVENTS로 거절하고 남겨 둔다")
  void 참여_기록이_있으면_삭제를_거절한다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);
    eventRepository.register(
        CampaignContentEventLog.builder()
            .campaignContentId(content.getId())
            .eventType(CampaignContentEventType.VIEW)
            .occurredAt(LocalDateTime.now(FIXED_CLOCK))
            .build());

    assertThatThrownBy(() -> service.delete(content.getId()))
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_HAS_EVENTS);
    assertThat(campaignContentRepository.findById(content.getId())).isPresent();
  }

  @Test
  @DisplayName("참여 기록이 없는 캠페인 콘텐츠는 삭제한다")
  void 참여_기록이_없으면_삭제할_수_있다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);

    service.delete(content.getId());

    assertThat(campaignContentRepository.findById(content.getId())).isEmpty();
  }

  @Test
  @DisplayName("목록을 조회할 때 최근 7일 결과 조회 회원 수를 붙이고 기록이 없으면 0이다")
  void 목록에_최근_7일_참여자를_붙인다() {
    CampaignContent mbti = persist("whiskey-mbti", "위스키 MBTI", true);
    CampaignContent tarot = persist("whiskey-tarot", "위스키 타로", true);
    metricsRepository.seedResultMembers(mbti.getId(), 12L);
    visitorStatisticsFacade.setExclusion(new VisitorExclusionItem(List.of("봇"), List.of("10.")));

    GlobalResponse response =
        service.search(new AdminCampaignContentSearchRequest(null, null, 0, 20));

    @SuppressWarnings("unchecked")
    List<AdminCampaignContentListResponse> items =
        (List<AdminCampaignContentListResponse>) response.getData();
    assertThat(items)
        .extracting(
            AdminCampaignContentListResponse::code,
            AdminCampaignContentListResponse::recentParticipants)
        .containsExactly(tuple(tarot.getCode(), 0L), tuple(mbti.getCode(), 12L));
    assertThat(metricsRepository.lastFrom()).isEqualTo(TODAY.minusDays(6).atStartOfDay());
    assertThat(metricsRepository.lastToExclusive()).isEqualTo(TODAY.plusDays(1).atStartOfDay());
    assertThat(metricsRepository.lastExclusion().deviceTypes()).containsExactly("봇");
    assertThat(metricsRepository.lastExclusion().ipPrefixes()).containsExactly("10.");
  }

  @Test
  @DisplayName("지표를 조회할 때 완주율·로그인 전환율·참여율을 같은 식별자 공간에서 소수 1자리로 계산한다")
  void 참여_지표를_계산할_수_있다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);
    metricsRepository.seedCounts(
        content.getId(), new CampaignContentEventCounts(4120L, 2730L, 1905L, 1284L, 1900L, 1280L));
    visitorStatisticsFacade.setActiveMembers(10439L);
    AdminCampaignContentMetricsResponse metrics =
        service.getMetrics(
            content.getId(), new AdminCampaignContentMetricsRequest(TODAY.minusDays(6), TODAY));

    assertThat(metrics.viewVisitors()).isEqualTo(4120L);
    assertThat(metrics.resultMembers()).isEqualTo(1284L);
    assertThat(metrics.activeMembers()).isEqualTo(10439L);
    assertThat(metrics.completionRate()).isEqualTo(69.6);
    assertThat(metrics.loginConversionRate()).isEqualTo(67.2);
    assertThat(metrics.participationRate()).isEqualTo(12.3);
    assertThat(visitorStatisticsFacade.lastFrom()).isEqualTo(TODAY.minusDays(6).atStartOfDay());
    assertThat(visitorStatisticsFacade.lastToExclusive())
        .isEqualTo(TODAY.plusDays(1).atStartOfDay());
  }

  @Test
  @DisplayName("이벤트가 없어 분모가 0일 때 비율을 0으로 돌려준다")
  void 분모가_0이면_비율은_0이다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);

    AdminCampaignContentMetricsResponse metrics =
        service.getMetrics(content.getId(), new AdminCampaignContentMetricsRequest(null, null));

    assertThat(metrics.completionRate()).isZero();
    assertThat(metrics.loginConversionRate()).isZero();
    assertThat(metrics.participationRate()).isZero();
    assertThat(metrics.to()).isEqualTo(TODAY);
    assertThat(metrics.from()).isEqualTo(TODAY.minusDays(6));
  }

  @Test
  @DisplayName("오늘에서 90일 전부터 조회할 때 INVALID_METRICS_RANGE로 거절한다")
  void 오늘에서_90일_전은_거절한다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);

    assertThatThrownBy(
            () ->
                service.getMetrics(
                    content.getId(),
                    new AdminCampaignContentMetricsRequest(TODAY.minusDays(90), TODAY)))
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_INVALID_METRICS_RANGE);
  }

  @Test
  @DisplayName("내일까지 조회할 때 INVALID_METRICS_RANGE로 거절한다")
  void 미래_기간은_거절한다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);

    assertThatThrownBy(
            () ->
                service.getMetrics(
                    content.getId(),
                    new AdminCampaignContentMetricsRequest(TODAY, TODAY.plusDays(1))))
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_INVALID_METRICS_RANGE);
  }

  @Test
  @DisplayName("시작일이 종료일보다 늦을 때 INVALID_METRICS_RANGE로 거절한다")
  void 뒤집힌_기간은_거절한다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);

    assertThatThrownBy(
            () ->
                service.getMetrics(
                    content.getId(),
                    new AdminCampaignContentMetricsRequest(TODAY, TODAY.minusDays(1))))
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_INVALID_METRICS_RANGE);
  }

  @Test
  @DisplayName("오늘을 포함한 90일 기간은 지표를 조회할 수 있다")
  void 보존_기간_경계인_90일은_조회할_수_있다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);

    AdminCampaignContentMetricsResponse metrics =
        service.getMetrics(
            content.getId(), new AdminCampaignContentMetricsRequest(TODAY.minusDays(89), TODAY));

    assertThat(metrics.from()).isEqualTo(TODAY.minusDays(89));
  }

  @Test
  @DisplayName("KST 23시 59분 59초에는 해당 날짜를 오늘로 사용한다")
  void 자정_직전에는_해당_날짜를_사용한다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);
    AdminCampaignContentService serviceAtBoundary =
        serviceWithClock(Clock.fixed(Instant.parse("2026-09-15T14:59:59Z"), ZONE));

    AdminCampaignContentMetricsResponse metrics =
        serviceAtBoundary.getMetrics(
            content.getId(), new AdminCampaignContentMetricsRequest(null, null));

    assertThat(metrics.to()).isEqualTo(LocalDate.of(2026, 9, 15));
  }

  @Test
  @DisplayName("KST 00시 00분 00초에는 다음 날짜를 오늘로 사용한다")
  void 자정부터는_다음_날짜를_사용한다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);
    AdminCampaignContentService serviceAtBoundary =
        serviceWithClock(Clock.fixed(Instant.parse("2026-09-15T15:00:00Z"), ZONE));

    AdminCampaignContentMetricsResponse metrics =
        serviceAtBoundary.getMetrics(
            content.getId(), new AdminCampaignContentMetricsRequest(null, null));

    assertThat(metrics.to()).isEqualTo(LocalDate.of(2026, 9, 16));
  }

  @Test
  @DisplayName("UTC zone의 Clock을 주입해도 KST 날짜를 오늘로 사용한다")
  void UTC_Clock을_주입해도_KST_날짜를_사용한다() {
    CampaignContent content = persist("whiskey-mbti", "위스키 MBTI", true);
    AdminCampaignContentService serviceWithUtcClock =
        serviceWithClock(Clock.fixed(Instant.parse("2026-09-15T15:00:00Z"), ZoneId.of("UTC")));

    AdminCampaignContentMetricsResponse metrics =
        serviceWithUtcClock.getMetrics(
            content.getId(), new AdminCampaignContentMetricsRequest(null, null));

    assertThat(metrics.to()).isEqualTo(LocalDate.of(2026, 9, 16));
  }

  private AdminCampaignContentService serviceWithClock(Clock clock) {
    return new AdminCampaignContentService(
        campaignContentRepository,
        eventRepository,
        metricsRepository,
        visitorStatisticsFacade,
        clock);
  }

  private CampaignContent persist(String code, String name, boolean isActive) {
    return campaignContentRepository.register(
        CampaignContent.builder().code(code).name(name).isActive(isActive).build());
  }
}
