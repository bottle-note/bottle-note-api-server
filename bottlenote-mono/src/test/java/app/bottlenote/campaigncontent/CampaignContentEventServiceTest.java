package app.bottlenote.campaigncontent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.campaigncontent.constant.CampaignContentEventType;
import app.bottlenote.campaigncontent.domain.CampaignContent;
import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import app.bottlenote.campaigncontent.dto.request.CampaignContentEventContextRequest;
import app.bottlenote.campaigncontent.dto.response.CampaignContentEventResponse;
import app.bottlenote.campaigncontent.exception.CampaignContentException;
import app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode;
import app.bottlenote.campaigncontent.fixture.InMemoryCampaignContentEventRepository;
import app.bottlenote.campaigncontent.fixture.InMemoryCampaignContentRepository;
import app.bottlenote.campaigncontent.service.CampaignContentEventService;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag("unit")
@DisplayName("캠페인 콘텐츠 참여 이벤트 서비스")
class CampaignContentEventServiceTest {

  private static final String VISITOR_ID = "a".repeat(64);
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-09-15T06:00:00Z"), ZoneId.of("UTC"));

  private InMemoryCampaignContentRepository campaignContentRepository;
  private InMemoryCampaignContentEventRepository eventRepository;
  private CampaignContentEventService service;

  @BeforeEach
  void setUp() {
    campaignContentRepository = new InMemoryCampaignContentRepository();
    eventRepository = new InMemoryCampaignContentEventRepository();
    service =
        new CampaignContentEventService(campaignContentRepository, eventRepository, FIXED_CLOCK);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = CampaignContentEventType.class,
      names = {"VIEW", "START", "FINISH"})
  @DisplayName("비로그인 사용자가 결과 이전 단계 이벤트를 보낼 때 방문자 기준으로 기록한다")
  void 비로그인_사용자의_결과_이전_이벤트를_기록할_수_있다(CampaignContentEventType type) {
    CampaignContent content = persist("whiskey-mbti", true);

    CampaignContentEventResponse response =
        service.registerEvent("whiskey-mbti", type, anonymous());

    CampaignContentEventLog saved = eventRepository.events().get(0);
    assertThat(saved.getCampaignContentId()).isEqualTo(content.getId());
    assertThat(saved.getEventType()).isEqualTo(type);
    assertThat(saved.getVisitorId()).isEqualTo(VISITOR_ID);
    assertThat(saved.getUserId()).isNull();
    assertThat(saved.getIpAddress()).isEqualTo("203.0.113.10");
    assertThat(saved.getDeviceType()).isEqualTo("모바일");
    assertThat(saved.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 15, 0));
    assertThat(response.code()).isEqualTo("whiskey-mbti");
    assertThat(response.type()).isEqualTo(type);
    assertThat(response.occurredAt()).isEqualTo(saved.getOccurredAt());
  }

  @Test
  @DisplayName("로그인 사용자가 RESULT를 보낼 때 회원과 방문자를 함께 기록한다")
  void 로그인_사용자의_결과_조회를_기록할_수_있다() {
    persist("whiskey-tarot", true);

    service.registerEvent(
        "whiskey-tarot",
        CampaignContentEventType.RESULT,
        new CampaignContentEventContextRequest(7L, VISITOR_ID, "203.0.113.10", "모바일"));

    CampaignContentEventLog saved = eventRepository.events().get(0);
    assertThat(saved.getUserId()).isEqualTo(7L);
    assertThat(saved.getVisitorId()).isEqualTo(VISITOR_ID);
  }

  @Test
  @DisplayName("비로그인 사용자가 RESULT를 보낼 때 REQUIRED_USER_ID로 거절하고 기록하지 않는다")
  void 비로그인_결과_조회는_거절한다() {
    persist("whiskey-tarot", true);

    assertThatThrownBy(
            () ->
                service.registerEvent(
                    "whiskey-tarot", CampaignContentEventType.RESULT, anonymous()))
        .isInstanceOf(UserException.class)
        .extracting("exceptionCode")
        .isEqualTo(UserExceptionCode.REQUIRED_USER_ID);
    assertThat(eventRepository.events()).isEmpty();
  }

  @Test
  @DisplayName("등록되지 않은 코드로 보낼 때 NOT_FOUND로 거절한다")
  void 미등록_코드는_거절한다() {
    assertThatThrownBy(
            () -> service.registerEvent("unknown", CampaignContentEventType.VIEW, anonymous()))
        .isInstanceOf(CampaignContentException.class)
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_NOT_FOUND);
  }

  @Test
  @DisplayName("비활성 코드로 보낼 때 미등록 코드와 같이 NOT_FOUND로 거절하고 기록하지 않는다")
  void 비활성_코드는_미등록과_같이_거절한다() {
    persist("whiskey-mbti", false);

    assertThatThrownBy(
            () -> service.registerEvent("whiskey-mbti", CampaignContentEventType.VIEW, anonymous()))
        .isInstanceOf(CampaignContentException.class)
        .extracting("exceptionCode")
        .isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_NOT_FOUND);
    assertThat(eventRepository.events()).isEmpty();
  }

  private CampaignContent persist(String code, boolean isActive) {
    return campaignContentRepository.save(
        CampaignContent.builder().code(code).name(code).isActive(isActive).build());
  }

  private CampaignContentEventContextRequest anonymous() {
    return new CampaignContentEventContextRequest(null, VISITOR_ID, "203.0.113.10", "모바일");
  }
}
