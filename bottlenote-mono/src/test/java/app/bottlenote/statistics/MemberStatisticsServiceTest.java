package app.bottlenote.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.timeseries.TimeSeriesException;
import app.bottlenote.global.timeseries.TimeSeriesExceptionCode;
import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.statistics.constant.MemberFunnelStage;
import app.bottlenote.statistics.domain.MemberCohort;
import app.bottlenote.statistics.domain.MemberCohortActivity;
import app.bottlenote.statistics.domain.MemberFunnelCounts;
import app.bottlenote.statistics.dto.request.MemberCohortRetentionRequest;
import app.bottlenote.statistics.dto.request.MemberFunnelRequest;
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse;
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse.CohortItem;
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse.PointItem;
import app.bottlenote.statistics.dto.response.MemberFunnelResponse;
import app.bottlenote.statistics.fixture.InMemoryMemberCohortRetentionRepository;
import app.bottlenote.statistics.fixture.InMemoryMemberFunnelRepository;
import app.bottlenote.statistics.service.MemberStatisticsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("회원 통계 서비스")
class MemberStatisticsServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private InMemoryMemberFunnelRepository funnelRepository;
  private InMemoryMemberCohortRetentionRepository cohortRepository;
  private MemberStatisticsService service;
  private LocalDate today;

  @BeforeEach
  void setUp() {
    funnelRepository = new InMemoryMemberFunnelRepository();
    cohortRepository = new InMemoryMemberCohortRetentionRepository();
    service = new MemberStatisticsService(funnelRepository, cohortRepository);
    today = LocalDate.now(ZONE);
  }

  @Nested
  @DisplayName("전환 퍼널")
  class Funnel {

    @Test
    @DisplayName("단계별 인원으로 직전 단계 대비 전환율과 방문 대비 누적 전환율을 계산한다")
    void 단계별_전환율을_계산할_수_있다() {
      funnelRepository.seed(new MemberFunnelCounts(200L, 30L, 25L, 10L));

      MemberFunnelResponse response =
          service.findFunnel(new MemberFunnelRequest(today.minusDays(6), today));

      assertThat(response.stages())
          .extracting(MemberFunnelResponse.StageItem::stage)
          .containsExactly(
              MemberFunnelStage.VISITED,
              MemberFunnelStage.SIGNED_UP,
              MemberFunnelStage.AGREED,
              MemberFunnelStage.ACTIVATED);
      assertThat(response.stages())
          .extracting(MemberFunnelResponse.StageItem::count)
          .containsExactly(200L, 30L, 25L, 10L);
      assertThat(response.stages())
          .extracting(MemberFunnelResponse.StageItem::conversionRate)
          .containsExactly(null, 15.0, 83.3, 40.0);
      assertThat(response.stages())
          .extracting(MemberFunnelResponse.StageItem::overallRate)
          .containsExactly(100.0, 15.0, 12.5, 5.0);
      assertThat(response.stages().get(2).label()).isEqualTo("필수 약관 동의");
    }

    @Test
    @DisplayName("방문자가 0명이면 모든 비율을 0으로 내리고 예외를 내지 않는다")
    void 방문자가_없으면_비율을_0으로_낼_수_있다() {
      funnelRepository.seed(MemberFunnelCounts.empty());

      MemberFunnelResponse response = service.findFunnel(new MemberFunnelRequest(today, today));

      assertThat(response.stages().get(0).overallRate()).isEqualTo(0.0);
      assertThat(response.stages().get(1).conversionRate()).isEqualTo(0.0);
      assertThat(response.stages().get(3).overallRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("from과 to를 생략하면 오늘 기준 최근 30일을 일 단위 경계로 조회한다")
    void 기본_구간은_최근_30일이다() {
      service.findFunnel(new MemberFunnelRequest(null, null));

      assertThat(funnelRepository.lastFrom()).isEqualTo(today.minusDays(29).atStartOfDay());
      assertThat(funnelRepository.lastToExclusive()).isEqualTo(today.plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("91일 구간을 요청하면 RANGE_TOO_LONG으로 거절한다")
    void 구간_상한을_넘으면_거절할_수_있다() {
      assertThatThrownBy(
              () -> service.findFunnel(new MemberFunnelRequest(today.minusDays(90), today)))
          .isInstanceOf(TimeSeriesException.class)
          .hasMessage(TimeSeriesExceptionCode.RANGE_TOO_LONG.getMessage());
    }
  }

  @Nested
  @DisplayName("가입 코호트 리텐션")
  class CohortRetention {

    @Test
    @DisplayName("가입자가 없는 버킷은 0명으로 채우고 offset별 재방문율을 소수 1자리로 계산한다")
    void 코호트_행렬을_조립할_수_있다() {
      LocalDate from = today.minusDays(2);
      LocalDateTime firstCohort = from.atStartOfDay();
      LocalDateTime thirdCohort = today.atStartOfDay();
      cohortRepository.seedCohorts(
          new MemberCohort(firstCohort, 3L), new MemberCohort(thirdCohort, 5L));
      cohortRepository.seedActivities(
          new MemberCohortActivity(firstCohort, 0, 3L),
          new MemberCohortActivity(firstCohort, 1, 2L),
          new MemberCohortActivity(firstCohort, 2, 1L),
          new MemberCohortActivity(thirdCohort, 0, 4L));
      cohortRepository.seedEarliestActivityDate(today.minusDays(40));

      MemberCohortRetentionResponse response =
          service.findCohortRetention(
              new MemberCohortRetentionRequest(from, today, TimeSeriesGranularity.DAY, 3));

      assertThat(response.granularity()).isEqualTo(TimeSeriesGranularity.DAY);
      assertThat(response.offsets()).isEqualTo(3);
      assertThat(response.coverageFrom()).isEqualTo(today.minusDays(40).toString());
      assertThat(response.cohorts()).hasSize(3);

      CohortItem first = response.cohorts().get(0);
      assertThat(first.members()).isEqualTo(3L);
      assertThat(first.points()).extracting(PointItem::activeMembers).containsExactly(3L, 2L, 1L);
      assertThat(first.points())
          .extracting(PointItem::retentionRate)
          .containsExactly(100.0, 66.7, 33.3);
      assertThat(first.points()).extracting(PointItem::partial).containsExactly(false, false, true);

      CohortItem second = response.cohorts().get(1);
      assertThat(second.members()).isZero();
      assertThat(second.points().get(0).activeMembers()).isZero();
      assertThat(second.points().get(0).retentionRate()).isNull();
    }

    @Test
    @DisplayName("아직 시작되지 않은 offset 버킷은 값이 null이고 partial이다")
    void 미래_버킷은_null로_낼_수_있다() {
      LocalDateTime cohortAt = today.atStartOfDay();
      cohortRepository.seedCohorts(new MemberCohort(cohortAt, 2L));
      cohortRepository.seedActivities(new MemberCohortActivity(cohortAt, 0, 2L));

      MemberCohortRetentionResponse response =
          service.findCohortRetention(
              new MemberCohortRetentionRequest(today, today, TimeSeriesGranularity.DAY, 2));

      PointItem future = response.cohorts().get(0).points().get(1);
      assertThat(future.offset()).isEqualTo(1);
      assertThat(future.activeMembers()).isNull();
      assertThat(future.retentionRate()).isNull();
      assertThat(future.partial()).isTrue();
    }

    @Test
    @DisplayName("offsets를 생략하면 단위별 기본값을 쓰고 24를 넘으면 24로 자른다")
    void offsets_기본값과_상한을_적용할_수_있다() {
      assertThat(offsetsRequested(TimeSeriesGranularity.DAY, null)).isEqualTo(14);
      assertThat(offsetsRequested(TimeSeriesGranularity.WEEK, null)).isEqualTo(8);
      assertThat(offsetsRequested(TimeSeriesGranularity.MONTH, null)).isEqualTo(6);
      assertThat(offsetsRequested(TimeSeriesGranularity.WEEK, 99)).isEqualTo(24);
      assertThat(offsetsRequested(TimeSeriesGranularity.WEEK, 0)).isEqualTo(1);
    }

    private int offsetsRequested(TimeSeriesGranularity granularity, Integer offsets) {
      MemberCohortRetentionResponse response =
          service.findCohortRetention(
              new MemberCohortRetentionRequest(today, today, granularity, offsets));
      assertThat(cohortRepository.lastMaxOffsetExclusive()).isEqualTo(response.offsets());
      return response.offsets();
    }

    @Test
    @DisplayName("granularity를 생략하면 WEEK로 조회하고 offsets를 레포지토리에 넘긴다")
    void 기본_단위는_WEEK이다() {
      service.findCohortRetention(new MemberCohortRetentionRequest(null, null, null, null));

      assertThat(cohortRepository.lastGranularity()).isEqualTo(TimeSeriesGranularity.WEEK);
      assertThat(cohortRepository.lastMaxOffsetExclusive()).isEqualTo(8);
    }

    @Test
    @DisplayName("HOUR 단위는 UNSUPPORTED_GRANULARITY로 거절한다")
    void HOUR는_거절할_수_있다() {
      assertThatThrownBy(
              () ->
                  service.findCohortRetention(
                      new MemberCohortRetentionRequest(
                          today, today, TimeSeriesGranularity.HOUR, null)))
          .isInstanceOf(TimeSeriesException.class)
          .hasMessage(TimeSeriesExceptionCode.UNSUPPORTED_GRANULARITY.getMessage());
    }
  }
}
