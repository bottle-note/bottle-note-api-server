package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.constant.PopularityAxis;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholInterestObservation;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshot;
import app.bottlenote.alcohols.domain.AlcoholRatingObservation;
import app.bottlenote.alcohols.dto.request.AlcoholPopularityTimeSeriesRequest;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholEngagementObservationRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholInterestObservationRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholPickObservationRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholPopularitySnapshotRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholRatingObservationRepository;
import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.global.timeseries.TimeSeriesDescriptor;
import app.bottlenote.global.timeseries.TimeSeriesException;
import app.bottlenote.global.timeseries.TimeSeriesExceptionCode;
import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.global.timeseries.TimeSeriesPoint;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("주류 인기도 시계열 서비스")
class AlcoholPopularityTimeSeriesServiceTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 9, 15, 0);
  private static final LocalDateTime CLOSED_WEEK_1 = LocalDateTime.of(2026, 8, 24, 0, 0);
  private static final LocalDateTime CLOSED_WEEK_2 = LocalDateTime.of(2026, 8, 31, 0, 0);
  private static final LocalDateTime OPEN_WEEK = LocalDateTime.of(2026, 9, 7, 0, 0);
  private static final LocalDate FROM = LocalDate.of(2026, 8, 24);
  private static final LocalDate TO = LocalDate.of(2026, 9, 9);

  private InMemoryAlcoholQueryRepository alcoholRepository;
  private InMemoryAlcoholPopularitySnapshotRepository snapshotRepository;
  private InMemoryAlcoholInterestObservationRepository interestRepository;
  private InMemoryAlcoholRatingObservationRepository ratingRepository;
  private InMemoryAlcoholPickObservationRepository pickRepository;
  private InMemoryAlcoholEngagementObservationRepository engagementRepository;
  private AlcoholPopularityTimeSeriesService service;
  private Long alcoholId;

  @BeforeEach
  void setUp() {
    alcoholRepository = new InMemoryAlcoholQueryRepository();
    snapshotRepository = new InMemoryAlcoholPopularitySnapshotRepository();
    interestRepository = new InMemoryAlcoholInterestObservationRepository();
    ratingRepository = new InMemoryAlcoholRatingObservationRepository();
    pickRepository = new InMemoryAlcoholPickObservationRepository();
    engagementRepository = new InMemoryAlcoholEngagementObservationRepository();
    service =
        new AlcoholPopularityTimeSeriesService(
            alcoholRepository,
            snapshotRepository,
            interestRepository,
            ratingRepository,
            pickRepository,
            engagementRepository,
            Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL));
    alcoholId = alcoholRepository.save(createAlcohol()).getId();
  }

  @Test
  @DisplayName("닫힌 WEEK 버킷은 배치 행 값을 그대로 내린다")
  void 닫힌_WEEK_버킷을_배치_행_그대로_내릴_수_있다() {
    snapshotRepository.save(
        snapshot(
            CLOSED_WEEK_1,
            BucketGranularity.WEEK,
            10L,
            20L,
            3L,
            8L,
            "0.10",
            "0.20",
            "0.30",
            "0.40",
            "0.50"));
    snapshotRepository.save(
        snapshot(
            CLOSED_WEEK_2,
            BucketGranularity.WEEK,
            11L,
            21L,
            4L,
            9L,
            "0.11",
            "0.21",
            "0.31",
            "0.41",
            "0.51"));

    TimeSeries series = service.findPopularity(alcoholId, weekRequest());

    TimeSeriesPoint first = pointAt(series, CLOSED_WEEK_1);
    assertThat(first.partial()).isFalse();
    assertThat(first.values().get("interestValue")).isEqualTo(10L);
    assertThat(first.values().get("ratingValue")).isEqualTo(20L);
    assertThat(first.values().get("pickValue")).isEqualTo(3L);
    assertThat(first.values().get("engagementValue")).isEqualTo(8L);
    assertThat(first.values().get("popularityScore")).isEqualTo(new BigDecimal("0.50"));
    assertThat(first.values().get("interestScore")).isEqualTo(new BigDecimal("0.10"));

    TimeSeriesPoint second = pointAt(series, CLOSED_WEEK_2);
    assertThat(second.partial()).isFalse();
    assertThat(second.values().get("interestValue")).isEqualTo(11L);
    assertThat(second.values().get("popularityScore")).isEqualTo(new BigDecimal("0.51"));
  }

  @Test
  @DisplayName("열린 WEEK 버킷은 HOUR 합·최신 상태값·점수 null·partial true다")
  void 열린_WEEK_버킷을_HOUR에서_롤다운할_수_있다() {
    snapshotRepository.save(
        snapshot(
            CLOSED_WEEK_2,
            BucketGranularity.WEEK,
            100L,
            20L,
            5L,
            8L,
            "0.10",
            "0.20",
            "0.30",
            "0.40",
            "0.50"));
    snapshotRepository.save(
        snapshot(
            LocalDateTime.of(2026, 9, 7, 10, 0),
            BucketGranularity.HOUR,
            3L,
            21L,
            6L,
            9L,
            "0.01",
            "0.02",
            "0.03",
            "0.04",
            "0.05"));
    snapshotRepository.save(
        snapshot(
            LocalDateTime.of(2026, 9, 8, 11, 0),
            BucketGranularity.HOUR,
            7L,
            24L,
            8L,
            12L,
            "0.06",
            "0.07",
            "0.08",
            "0.09",
            "0.10"));

    TimeSeries series = service.findPopularity(alcoholId, weekRequest());

    TimeSeriesPoint open = pointAt(series, OPEN_WEEK);
    assertThat(open.partial()).isTrue();
    assertThat(open.values().get("interestValue")).isEqualTo(10L);
    assertThat(open.values().get("ratingValue")).isEqualTo(24L);
    assertThat(open.values().get("pickValue")).isEqualTo(8L);
    assertThat(open.values().get("engagementValue")).isEqualTo(12L);
    assertThat(open.values().get("popularityScore")).isNull();
    assertThat(open.values().get("interestScore")).isNull();
    assertThat(open.values().get("ratingScore")).isNull();
    assertThat(open.values().get("pickScore")).isNull();
    assertThat(open.values().get("engagementScore")).isNull();
  }

  @Test
  @DisplayName("열린 WEEK에 HOUR 행이 없으면 흐름값은 0, 상태값은 직전 닫힌 버킷을 이어받는다")
  void 열린_WEEK에_HOUR가_없으면_PREVIOUS를_이어받을_수_있다() {
    snapshotRepository.save(
        snapshot(
            CLOSED_WEEK_2,
            BucketGranularity.WEEK,
            100L,
            20L,
            5L,
            8L,
            "0.10",
            "0.20",
            "0.30",
            "0.40",
            "0.50"));

    TimeSeries series = service.findPopularity(alcoholId, weekRequest());

    TimeSeriesPoint open = pointAt(series, OPEN_WEEK);
    assertThat(open.partial()).isTrue();
    assertThat(open.values().get("interestValue")).isEqualTo(0L);
    assertThat(open.values().get("ratingValue")).isEqualTo(20L);
    assertThat(open.values().get("pickValue")).isEqualTo(5L);
    assertThat(open.values().get("engagementValue")).isEqualTo(8L);
    assertThat(open.values().get("popularityScore")).isNull();
  }

  @Test
  @DisplayName("HOUR 요청은 열린 버킷을 롤다운하지 않는다")
  void HOUR_요청은_롤다운하지_않을_수_있다() {
    LocalDateTime hour = LocalDateTime.of(2026, 9, 9, 10, 0);
    snapshotRepository.save(
        snapshot(
            hour, BucketGranularity.HOUR, 4L, 1L, 1L, 1L, "0.10", "0.20", "0.30", "0.40", "0.50"));
    snapshotRepository.save(
        snapshot(
            OPEN_WEEK,
            BucketGranularity.WEEK,
            99L,
            99L,
            99L,
            99L,
            "0.90",
            "0.90",
            "0.90",
            "0.90",
            "0.90"));

    TimeSeries series =
        service.findPopularity(
            alcoholId,
            new AlcoholPopularityTimeSeriesRequest(
                LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 9), TimeSeriesGranularity.HOUR));

    TimeSeriesPoint point = pointAt(series, hour);
    assertThat(point.values().get("interestValue")).isEqualTo(4L);
    assertThat(point.values().get("popularityScore")).isEqualTo(new BigDecimal("0.50"));
    assertThat(pointAt(series, LocalDateTime.of(2026, 9, 9, 15, 0)).partial()).isTrue();
    assertThat(pointAt(series, LocalDateTime.of(2026, 9, 9, 15, 0)).values().get("interestValue"))
        .isEqualTo(0L);
  }

  @Test
  @DisplayName("평균 평점은 합을 개수로 나누고 개수가 0이면 null이다")
  void averageRating을_계산할_수_있다() {
    ratingRepository.save(rating(CLOSED_WEEK_2, BucketGranularity.WEEK, 4L, "16.0", 4L, "16.0"));
    ratingRepository.save(
        rating(LocalDateTime.of(2026, 9, 7, 10, 0), BucketGranularity.HOUR, 0L, "0.0", 0L, "0.0"));
    ratingRepository.save(
        rating(
            LocalDateTime.of(2026, 9, 8, 12, 0), BucketGranularity.HOUR, 5L, "18.5", 5L, "18.5"));

    TimeSeries withSample =
        service.findObservations(alcoholId, PopularityAxis.RATING, weekRequest());
    assertThat(pointAt(withSample, OPEN_WEEK).values().get("averageRating"))
        .isEqualTo(new BigDecimal("3.70"));
    assertThat(pointAt(withSample, CLOSED_WEEK_2).values().get("averageRating"))
        .isEqualTo(new BigDecimal("4.00"));

    ratingRepository.save(
        rating(LocalDateTime.of(2026, 9, 8, 12, 0), BucketGranularity.HOUR, 0L, "0.0", 0L, "0.0"));
    TimeSeries emptyCount =
        service.findObservations(alcoholId, PopularityAxis.RATING, weekRequest());
    assertThat(pointAt(emptyCount, OPEN_WEEK).values().get("averageRating"))
        .isEqualTo(new BigDecimal("4.00"));
  }

  @Test
  @DisplayName("축별 시리즈 key 목록이 계약 표와 같다")
  void 축별_시리즈_key가_표와_일치할_수_있다() {
    assertThat(keys(service.findPopularity(alcoholId, weekRequest())))
        .containsExactly(
            "popularityScore",
            "interestScore",
            "ratingScore",
            "pickScore",
            "engagementScore",
            "interestValue",
            "ratingValue",
            "pickValue",
            "engagementValue");
    assertThat(keys(service.findObservations(alcoholId, PopularityAxis.INTEREST, weekRequest())))
        .containsExactly("viewCount", "cumulativeViewCount");
    assertThat(keys(service.findObservations(alcoholId, PopularityAxis.RATING, weekRequest())))
        .containsExactly(
            "deltaRatingCount", "deltaRatingSum", "ratingCount", "ratingSum", "averageRating");
    assertThat(keys(service.findObservations(alcoholId, PopularityAxis.PICK, weekRequest())))
        .containsExactly("deltaPickCount", "pickCount", "unpickCount");
    assertThat(keys(service.findObservations(alcoholId, PopularityAxis.ENGAGEMENT, weekRequest())))
        .containsExactly(
            "deltaReviewCount",
            "deltaLikeCount",
            "deltaDislikeCount",
            "deltaReplyCount",
            "reviewCount",
            "likeCount",
            "dislikeCount",
            "replyCount");
  }

  @Test
  @DisplayName("DAY 요청은 UNSUPPORTED_GRANULARITY다")
  void DAY_요청을_거절할_수_있다() {
    assertThatThrownBy(
            () ->
                service.findPopularity(
                    alcoholId,
                    new AlcoholPopularityTimeSeriesRequest(FROM, TO, TimeSeriesGranularity.DAY)))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", TimeSeriesExceptionCode.UNSUPPORTED_GRANULARITY);
  }

  @Test
  @DisplayName("HOUR 32일은 RANGE_TOO_LONG이다")
  void HOUR_32일을_거절할_수_있다() {
    assertThatThrownBy(
            () ->
                service.findPopularity(
                    alcoholId,
                    new AlcoholPopularityTimeSeriesRequest(
                        LocalDate.of(2026, 8, 9), TO, TimeSeriesGranularity.HOUR)))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", TimeSeriesExceptionCode.RANGE_TOO_LONG);
  }

  @Test
  @DisplayName("WEEK 367일은 RANGE_TOO_LONG이다")
  void WEEK_367일을_거절할_수_있다() {
    assertThatThrownBy(
            () ->
                service.findPopularity(
                    alcoholId,
                    new AlcoholPopularityTimeSeriesRequest(
                        LocalDate.of(2025, 9, 8), TO, TimeSeriesGranularity.WEEK)))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", TimeSeriesExceptionCode.RANGE_TOO_LONG);
  }

  @Test
  @DisplayName("없는 주류는 ALCOHOL_NOT_FOUND다")
  void 없는_주류를_거절할_수_있다() {
    assertThatThrownBy(() -> service.findPopularity(999L, weekRequest()))
        .isInstanceOf(AlcoholException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", AlcoholExceptionCode.ALCOHOL_NOT_FOUND);
  }

  @Test
  @DisplayName("관심도 열린 버킷은 조회수 합과 최신 누적값을 내린다")
  void 관심도_열린_버킷을_롤다운할_수_있다() {
    interestRepository.save(interest(CLOSED_WEEK_2, BucketGranularity.WEEK, 10L, 100L));
    interestRepository.save(
        interest(LocalDateTime.of(2026, 9, 7, 10, 0), BucketGranularity.HOUR, 3L, 103L));
    interestRepository.save(
        interest(LocalDateTime.of(2026, 9, 8, 9, 0), BucketGranularity.HOUR, 4L, 107L));

    TimeSeries series = service.findObservations(alcoholId, PopularityAxis.INTEREST, weekRequest());

    TimeSeriesPoint open = pointAt(series, OPEN_WEEK);
    assertThat(open.partial()).isTrue();
    assertThat(open.values().get("viewCount")).isEqualTo(7L);
    assertThat(open.values().get("cumulativeViewCount")).isEqualTo(107L);
  }

  @Test
  @DisplayName("to가 오늘보다 뒤인 WEEK 요청은 INVALID_RANGE 예외를 던진다")
  void 미래_to_요청은_거절할_수_있다() {
    // 커널이 미래 to를 거절하므로 미래 주 버킷은 롤다운 대상이 될 수 없다.
    assertThatThrownBy(
            () ->
                service.findPopularity(
                    alcoholId,
                    new AlcoholPopularityTimeSeriesRequest(
                        LocalDate.of(2026, 9, 7),
                        LocalDate.of(2026, 9, 21),
                        TimeSeriesGranularity.WEEK)))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", TimeSeriesExceptionCode.INVALID_RANGE);
  }

  @Test
  @DisplayName("열린 WEEK가 첫 버킷이고 HOUR가 없으면 직전 닫힌 WEEK 상태값을 이어받는다")
  void 첫_열린_버킷_상태값을_직전_닫힌_행에서_이을_수_있다() {
    snapshotRepository.save(
        snapshot(
            CLOSED_WEEK_2,
            BucketGranularity.WEEK,
            100L,
            20L,
            5L,
            8L,
            "0.10",
            "0.20",
            "0.30",
            "0.40",
            "0.50"));
    ratingRepository.save(rating(CLOSED_WEEK_2, BucketGranularity.WEEK, 4L, "16.0", 4L, "16.0"));

    AlcoholPopularityTimeSeriesRequest openOnly =
        new AlcoholPopularityTimeSeriesRequest(
            LocalDate.of(2026, 9, 7), TO, TimeSeriesGranularity.WEEK);

    TimeSeries popularity = service.findPopularity(alcoholId, openOnly);
    TimeSeriesPoint popularityOpen = pointAt(popularity, OPEN_WEEK);
    assertThat(popularityOpen.partial()).isTrue();
    assertThat(popularityOpen.values().get("interestValue")).isEqualTo(0L);
    assertThat(popularityOpen.values().get("ratingValue")).isEqualTo(20L);
    assertThat(popularityOpen.values().get("pickValue")).isEqualTo(5L);
    assertThat(popularityOpen.values().get("engagementValue")).isEqualTo(8L);

    TimeSeries rating = service.findObservations(alcoholId, PopularityAxis.RATING, openOnly);
    TimeSeriesPoint ratingOpen = pointAt(rating, OPEN_WEEK);
    assertThat(ratingOpen.values().get("deltaRatingCount")).isEqualTo(0L);
    assertThat(ratingOpen.values().get("ratingCount")).isEqualTo(4L);
    assertThat(ratingOpen.values().get("ratingSum")).isEqualTo(new BigDecimal("16.0"));
    assertThat(ratingOpen.values().get("averageRating")).isEqualTo(new BigDecimal("4.00"));
  }

  @Test
  @DisplayName("닫힌 행도 HOUR 행도 없으면 상태값은 null이다")
  void 이어받을_닫힌_행이_없으면_상태값이_null일_수_있다() {
    TimeSeries series = service.findPopularity(alcoholId, weekRequest());

    TimeSeriesPoint open = pointAt(series, OPEN_WEEK);
    assertThat(open.values().get("interestValue")).isEqualTo(0L);
    assertThat(open.values().get("ratingValue")).isNull();
    assertThat(open.values().get("pickValue")).isNull();
    assertThat(open.values().get("engagementValue")).isNull();
    assertThat(open.values().get("popularityScore")).isNull();
  }

  @Test
  @DisplayName("HOUR 조회에서 평점 변화가 없는 시간은 직전 평균을 이어받는다")
  void HOUR_averageRating을_PREVIOUS로_이을_수_있다() {
    ratingRepository.save(
        rating(LocalDateTime.of(2026, 9, 9, 10, 0), BucketGranularity.HOUR, 4L, "16.0", 0L, "0.0"));

    TimeSeries series =
        service.findObservations(
            alcoholId,
            PopularityAxis.RATING,
            new AlcoholPopularityTimeSeriesRequest(
                LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 9), TimeSeriesGranularity.HOUR));

    assertThat(pointAt(series, LocalDateTime.of(2026, 9, 9, 10, 0)).values().get("averageRating"))
        .isEqualTo(new BigDecimal("4.00"));
    assertThat(pointAt(series, LocalDateTime.of(2026, 9, 9, 11, 0)).values().get("averageRating"))
        .isEqualTo(new BigDecimal("4.00"));
  }

  private AlcoholPopularityTimeSeriesRequest weekRequest() {
    return new AlcoholPopularityTimeSeriesRequest(FROM, TO, TimeSeriesGranularity.WEEK);
  }

  private TimeSeriesPoint pointAt(TimeSeries series, LocalDateTime bucketAt) {
    String formatted = TimeSeries.format(bucketAt);
    return series.points().stream()
        .filter(point -> formatted.equals(point.bucketAt()))
        .findFirst()
        .orElseThrow();
  }

  private List<String> keys(TimeSeries series) {
    return series.series().stream().map(TimeSeriesDescriptor::key).toList();
  }

  private Alcohol createAlcohol() {
    return Alcohol.builder()
        .korName("테스트 위스키")
        .engName("Test Whisky")
        .type(AlcoholType.WHISKY)
        .korCategory("위스키")
        .engCategory("Whisky")
        .categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
        .build();
  }

  private AlcoholPopularitySnapshot snapshot(
      LocalDateTime bucketAt,
      BucketGranularity granularity,
      Long interestValue,
      Long ratingValue,
      Long pickValue,
      Long engagementValue,
      String interestScore,
      String ratingScore,
      String pickScore,
      String engagementScore,
      String popularityScore) {
    return AlcoholPopularitySnapshot.builder()
        .alcoholId(alcoholId)
        .bucketGranularity(granularity)
        .bucketAt(bucketAt)
        .observedAt(bucketAt.plusHours(1))
        .interestValue(interestValue)
        .interestScore(new BigDecimal(interestScore))
        .ratingValue(ratingValue)
        .ratingScore(new BigDecimal(ratingScore))
        .pickValue(pickValue)
        .pickScore(new BigDecimal(pickScore))
        .engagementValue(engagementValue)
        .engagementScore(new BigDecimal(engagementScore))
        .popularityScore(new BigDecimal(popularityScore))
        .build();
  }

  private AlcoholInterestObservation interest(
      LocalDateTime bucketAt,
      BucketGranularity granularity,
      Long viewCount,
      Long cumulativeViewCount) {
    return AlcoholInterestObservation.builder()
        .alcoholId(alcoholId)
        .bucketGranularity(granularity)
        .bucketAt(bucketAt)
        .observedAt(bucketAt.plusHours(1))
        .viewCount(viewCount)
        .cumulativeViewCount(cumulativeViewCount)
        .build();
  }

  private AlcoholRatingObservation rating(
      LocalDateTime bucketAt,
      BucketGranularity granularity,
      Long ratingCount,
      String ratingSum,
      Long deltaRatingCount,
      String deltaRatingSum) {
    return AlcoholRatingObservation.builder()
        .alcoholId(alcoholId)
        .bucketGranularity(granularity)
        .bucketAt(bucketAt)
        .observedAt(bucketAt.plusHours(1))
        .ratingCount(ratingCount)
        .ratingSum(new BigDecimal(ratingSum))
        .deltaRatingCount(deltaRatingCount)
        .deltaRatingSum(new BigDecimal(deltaRatingSum))
        .build();
  }
}
