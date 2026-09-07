package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static java.lang.Boolean.FALSE;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.constant.PopularityAxis;
import app.bottlenote.alcohols.domain.AlcoholEngagementObservation;
import app.bottlenote.alcohols.domain.AlcoholEngagementObservationRepository;
import app.bottlenote.alcohols.domain.AlcoholInterestObservation;
import app.bottlenote.alcohols.domain.AlcoholInterestObservationRepository;
import app.bottlenote.alcohols.domain.AlcoholPickObservation;
import app.bottlenote.alcohols.domain.AlcoholPickObservationRepository;
import app.bottlenote.alcohols.domain.AlcoholPopularityBucketRepository;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshot;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshotRepository;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.AlcoholRatingObservation;
import app.bottlenote.alcohols.domain.AlcoholRatingObservationRepository;
import app.bottlenote.alcohols.dto.request.AlcoholPopularityTimeSeriesRequest;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.global.timeseries.TimeSeriesAssembler;
import app.bottlenote.global.timeseries.TimeSeriesDescriptor;
import app.bottlenote.global.timeseries.TimeSeriesFill;
import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.global.timeseries.TimeSeriesRange;
import app.bottlenote.global.timeseries.TimeSeriesUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlcoholPopularityTimeSeriesService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final Set<TimeSeriesGranularity> SUPPORTED =
      EnumSet.of(
          TimeSeriesGranularity.HOUR, TimeSeriesGranularity.WEEK, TimeSeriesGranularity.MONTH);
  private static final List<TimeSeriesDescriptor> POPULARITY_SERIES =
      List.of(
          descriptor("popularityScore", "최종 인기도", TimeSeriesUnit.SCORE, TimeSeriesFill.NULL),
          descriptor("interestScore", "관심도 점수", TimeSeriesUnit.SCORE, TimeSeriesFill.NULL),
          descriptor("ratingScore", "평가도 점수", TimeSeriesUnit.SCORE, TimeSeriesFill.NULL),
          descriptor("pickScore", "선호도 점수", TimeSeriesUnit.SCORE, TimeSeriesFill.NULL),
          descriptor("engagementScore", "참여도 점수", TimeSeriesUnit.SCORE, TimeSeriesFill.NULL),
          descriptor("interestValue", "상세 조회 수", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
          descriptor("ratingValue", "누적 평점 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS),
          descriptor("pickValue", "PICK 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS),
          descriptor("engagementValue", "참여 합계", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS));
  private static final List<TimeSeriesDescriptor> INTEREST_SERIES =
      List.of(
          descriptor("viewCount", "상세 조회 수", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
          descriptor(
              "cumulativeViewCount", "누적 조회 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS));
  private static final List<TimeSeriesDescriptor> RATING_SERIES =
      List.of(
          descriptor("deltaRatingCount", "평점 수 증감", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
          descriptor("deltaRatingSum", "평점 합 증감", TimeSeriesUnit.DECIMAL, TimeSeriesFill.ZERO),
          descriptor("ratingCount", "누적 평점 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS),
          descriptor("ratingSum", "누적 평점 합", TimeSeriesUnit.DECIMAL, TimeSeriesFill.PREVIOUS),
          descriptor("averageRating", "평균 평점", TimeSeriesUnit.DECIMAL, TimeSeriesFill.NULL));
  private static final List<TimeSeriesDescriptor> PICK_SERIES =
      List.of(
          descriptor("deltaPickCount", "PICK 증감", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
          descriptor("pickCount", "PICK 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS),
          descriptor("unpickCount", "UNPICK 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS));
  private static final List<TimeSeriesDescriptor> ENGAGEMENT_SERIES =
      List.of(
          descriptor("deltaReviewCount", "리뷰 증감", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
          descriptor("deltaLikeCount", "좋아요 증감", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
          descriptor("deltaDislikeCount", "싫어요 증감", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
          descriptor("deltaReplyCount", "댓글 증감", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO),
          descriptor("reviewCount", "리뷰 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS),
          descriptor("likeCount", "좋아요 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS),
          descriptor("dislikeCount", "싫어요 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS),
          descriptor("replyCount", "댓글 수", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS));

  private final AlcoholQueryRepository alcoholQueryRepository;
  private final AlcoholPopularitySnapshotRepository snapshotRepository;
  private final AlcoholInterestObservationRepository interestRepository;
  private final AlcoholRatingObservationRepository ratingRepository;
  private final AlcoholPickObservationRepository pickRepository;
  private final AlcoholEngagementObservationRepository engagementRepository;
  private final Clock clock;

  @Autowired
  public AlcoholPopularityTimeSeriesService(
      AlcoholQueryRepository alcoholQueryRepository,
      AlcoholPopularitySnapshotRepository snapshotRepository,
      AlcoholInterestObservationRepository interestRepository,
      AlcoholRatingObservationRepository ratingRepository,
      AlcoholPickObservationRepository pickRepository,
      AlcoholEngagementObservationRepository engagementRepository) {
    this(
        alcoholQueryRepository,
        snapshotRepository,
        interestRepository,
        ratingRepository,
        pickRepository,
        engagementRepository,
        Clock.system(SEOUL));
  }

  AlcoholPopularityTimeSeriesService(
      AlcoholQueryRepository alcoholQueryRepository,
      AlcoholPopularitySnapshotRepository snapshotRepository,
      AlcoholInterestObservationRepository interestRepository,
      AlcoholRatingObservationRepository ratingRepository,
      AlcoholPickObservationRepository pickRepository,
      AlcoholEngagementObservationRepository engagementRepository,
      Clock clock) {
    this.alcoholQueryRepository = alcoholQueryRepository;
    this.snapshotRepository = snapshotRepository;
    this.interestRepository = interestRepository;
    this.ratingRepository = ratingRepository;
    this.pickRepository = pickRepository;
    this.engagementRepository = engagementRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public TimeSeries findPopularity(Long alcoholId, AlcoholPopularityTimeSeriesRequest request) {
    requireAlcohol(alcoholId);
    TimeSeriesRange range = resolveRange(request);
    LocalDateTime now = LocalDateTime.now(clock);
    List<AlcoholPopularitySnapshot> rows = loadRows(snapshotRepository, alcoholId, range);
    Map<LocalDateTime, Map<String, Number>> valuesByBucket =
        toValuesByBucket(
            rows,
            AlcoholPopularitySnapshot::getBucketAt,
            AlcoholPopularityTimeSeriesService::popularityValues,
            openBucketStart(range, now));
    rolldownIfOpen(
        range,
        now,
        valuesByBucket,
        openStart -> rolldownPopularity(alcoholId, openStart, range.granularity()));
    return TimeSeriesAssembler.assemble(range, POPULARITY_SERIES, valuesByBucket, now);
  }

  @Transactional(readOnly = true)
  public TimeSeries findObservations(
      Long alcoholId, PopularityAxis axis, AlcoholPopularityTimeSeriesRequest request) {
    requireAlcohol(alcoholId);
    Objects.requireNonNull(axis, "axis는 null일 수 없습니다.");
    return switch (axis) {
      case INTEREST ->
          assembleObservations(
              alcoholId,
              request,
              new ObservationAssembly<>(
                  interestRepository,
                  INTEREST_SERIES,
                  AlcoholInterestObservation::getBucketAt,
                  AlcoholPopularityTimeSeriesService::interestValues,
                  this::rolldownInterest));
      case RATING ->
          assembleObservations(
              alcoholId,
              request,
              new ObservationAssembly<>(
                  ratingRepository,
                  RATING_SERIES,
                  AlcoholRatingObservation::getBucketAt,
                  AlcoholPopularityTimeSeriesService::ratingValues,
                  this::rolldownRating));
      case PICK ->
          assembleObservations(
              alcoholId,
              request,
              new ObservationAssembly<>(
                  pickRepository,
                  PICK_SERIES,
                  AlcoholPickObservation::getBucketAt,
                  AlcoholPopularityTimeSeriesService::pickValues,
                  this::rolldownPick));
      case ENGAGEMENT ->
          assembleObservations(
              alcoholId,
              request,
              new ObservationAssembly<>(
                  engagementRepository,
                  ENGAGEMENT_SERIES,
                  AlcoholEngagementObservation::getBucketAt,
                  AlcoholPopularityTimeSeriesService::engagementValues,
                  this::rolldownEngagement));
    };
  }

  @FunctionalInterface
  private interface OpenBucketRolldown {
    Map<String, Number> apply(
        Long alcoholId, LocalDateTime start, TimeSeriesGranularity granularity);
  }

  private record ObservationAssembly<T>(
      AlcoholPopularityBucketRepository<T> repository,
      List<TimeSeriesDescriptor> series,
      Function<T, LocalDateTime> bucketAt,
      Function<T, Map<String, Number>> toValues,
      OpenBucketRolldown rolldown) {}

  private <T> TimeSeries assembleObservations(
      Long alcoholId, AlcoholPopularityTimeSeriesRequest request, ObservationAssembly<T> assembly) {
    TimeSeriesRange range = resolveRange(request);
    LocalDateTime now = LocalDateTime.now(clock);
    List<T> rows = loadRows(assembly.repository(), alcoholId, range);
    Map<LocalDateTime, Map<String, Number>> valuesByBucket =
        toValuesByBucket(
            rows, assembly.bucketAt(), assembly.toValues(), openBucketStart(range, now));
    rolldownIfOpen(
        range,
        now,
        valuesByBucket,
        openStart -> assembly.rolldown().apply(alcoholId, openStart, range.granularity()));
    return TimeSeriesAssembler.assemble(range, assembly.series(), valuesByBucket, now);
  }

  private void requireAlcohol(Long alcoholId) {
    if (FALSE.equals(alcoholQueryRepository.existsByAlcoholId(alcoholId))) {
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    }
  }

  private TimeSeriesRange resolveRange(AlcoholPopularityTimeSeriesRequest request) {
    Objects.requireNonNull(request, "request는 null일 수 없습니다.");
    TimeSeriesGranularity granularity = request.granularity();
    return TimeSeriesRange.of(
        request.from(),
        request.to(),
        granularity,
        SUPPORTED,
        maxDays(granularity),
        LocalDate.now(clock));
  }

  private int maxDays(TimeSeriesGranularity granularity) {
    return granularity == TimeSeriesGranularity.HOUR ? 31 : 366;
  }

  private BucketGranularity toBucket(TimeSeriesGranularity granularity) {
    return switch (granularity) {
      case HOUR -> BucketGranularity.HOUR;
      case WEEK -> BucketGranularity.WEEK;
      case MONTH -> BucketGranularity.MONTH;
      case DAY -> throw new IllegalStateException("DAY는 지원하지 않습니다.");
    };
  }

  private <T> List<T> loadRows(
      AlcoholPopularityBucketRepository<T> repository, Long alcoholId, TimeSeriesRange range) {
    return repository.findByAlcoholIdAndBucketGranularityAndBucketAtBetweenOrderByBucketAtAsc(
        alcoholId, toBucket(range.granularity()), range.from(), range.to());
  }

  private <T> Map<LocalDateTime, Map<String, Number>> toValuesByBucket(
      List<T> rows,
      Function<T, LocalDateTime> bucketAt,
      Function<T, Map<String, Number>> toValues,
      LocalDateTime openStart) {
    Map<LocalDateTime, Map<String, Number>> valuesByBucket = new LinkedHashMap<>();
    for (T row : rows) {
      LocalDateTime at = bucketAt.apply(row);
      if (openStart != null && at.equals(openStart)) {
        continue;
      }
      valuesByBucket.put(at, toValues.apply(row));
    }
    return valuesByBucket;
  }

  private void rolldownIfOpen(
      TimeSeriesRange range,
      LocalDateTime now,
      Map<LocalDateTime, Map<String, Number>> valuesByBucket,
      Function<LocalDateTime, Map<String, Number>> rolldown) {
    LocalDateTime openStart = openBucketStart(range, now);
    if (openStart == null) {
      return;
    }
    valuesByBucket.put(openStart, rolldown.apply(openStart));
  }

  private LocalDateTime openBucketStart(TimeSeriesRange range, LocalDateTime now) {
    if (range.granularity() == TimeSeriesGranularity.HOUR) {
      return null;
    }
    LocalDateTime last = range.to();
    if (range.granularity().next(last).isAfter(now)) {
      return last;
    }
    return null;
  }

  private Map<String, Number> rolldownPopularity(
      Long alcoholId, LocalDateTime start, TimeSeriesGranularity granularity) {
    List<AlcoholPopularitySnapshot> hours =
        loadHourRows(snapshotRepository, alcoholId, start, granularity);
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("interestValue", sumLong(hours, AlcoholPopularitySnapshot::getInterestValue));
    latest(hours)
        .ifPresent(
            last -> {
              values.put("ratingValue", last.getRatingValue());
              values.put("pickValue", last.getPickValue());
              values.put("engagementValue", last.getEngagementValue());
            });
    return values;
  }

  private Map<String, Number> rolldownInterest(
      Long alcoholId, LocalDateTime start, TimeSeriesGranularity granularity) {
    List<AlcoholInterestObservation> hours =
        loadHourRows(interestRepository, alcoholId, start, granularity);
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("viewCount", sumLong(hours, AlcoholInterestObservation::getViewCount));
    latest(hours)
        .ifPresent(last -> values.put("cumulativeViewCount", last.getCumulativeViewCount()));
    return values;
  }

  private Map<String, Number> rolldownRating(
      Long alcoholId, LocalDateTime start, TimeSeriesGranularity granularity) {
    List<AlcoholRatingObservation> hours =
        loadHourRows(ratingRepository, alcoholId, start, granularity);
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("deltaRatingCount", sumLong(hours, AlcoholRatingObservation::getDeltaRatingCount));
    values.put("deltaRatingSum", sumDecimal(hours, AlcoholRatingObservation::getDeltaRatingSum));
    latest(hours)
        .ifPresent(
            last -> {
              values.put("ratingCount", last.getRatingCount());
              values.put("ratingSum", last.getRatingSum());
              values.put(
                  "averageRating", averageRating(last.getRatingCount(), last.getRatingSum()));
            });
    return values;
  }

  private Map<String, Number> rolldownPick(
      Long alcoholId, LocalDateTime start, TimeSeriesGranularity granularity) {
    List<AlcoholPickObservation> hours =
        loadHourRows(pickRepository, alcoholId, start, granularity);
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("deltaPickCount", sumLong(hours, AlcoholPickObservation::getDeltaPickCount));
    latest(hours)
        .ifPresent(
            last -> {
              values.put("pickCount", last.getPickCount());
              values.put("unpickCount", last.getUnpickCount());
            });
    return values;
  }

  private Map<String, Number> rolldownEngagement(
      Long alcoholId, LocalDateTime start, TimeSeriesGranularity granularity) {
    List<AlcoholEngagementObservation> hours =
        loadHourRows(engagementRepository, alcoholId, start, granularity);
    Map<String, Number> values = new LinkedHashMap<>();
    values.put(
        "deltaReviewCount", sumLong(hours, AlcoholEngagementObservation::getDeltaReviewCount));
    values.put("deltaLikeCount", sumLong(hours, AlcoholEngagementObservation::getDeltaLikeCount));
    values.put(
        "deltaDislikeCount", sumLong(hours, AlcoholEngagementObservation::getDeltaDislikeCount));
    values.put("deltaReplyCount", sumLong(hours, AlcoholEngagementObservation::getDeltaReplyCount));
    latest(hours)
        .ifPresent(
            last -> {
              values.put("reviewCount", last.getReviewCount());
              values.put("likeCount", last.getLikeCount());
              values.put("dislikeCount", last.getDislikeCount());
              values.put("replyCount", last.getReplyCount());
            });
    return values;
  }

  private <T> List<T> loadHourRows(
      AlcoholPopularityBucketRepository<T> repository,
      Long alcoholId,
      LocalDateTime start,
      TimeSeriesGranularity granularity) {
    LocalDateTime hourTo = granularity.next(start).minusHours(1);
    return repository.findByAlcoholIdAndBucketGranularityAndBucketAtBetweenOrderByBucketAtAsc(
        alcoholId, BucketGranularity.HOUR, start, hourTo);
  }

  private static Map<String, Number> popularityValues(AlcoholPopularitySnapshot snapshot) {
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("popularityScore", snapshot.getPopularityScore());
    values.put("interestScore", snapshot.getInterestScore());
    values.put("ratingScore", snapshot.getRatingScore());
    values.put("pickScore", snapshot.getPickScore());
    values.put("engagementScore", snapshot.getEngagementScore());
    values.put("interestValue", snapshot.getInterestValue());
    values.put("ratingValue", snapshot.getRatingValue());
    values.put("pickValue", snapshot.getPickValue());
    values.put("engagementValue", snapshot.getEngagementValue());
    return values;
  }

  private static Map<String, Number> interestValues(AlcoholInterestObservation observation) {
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("viewCount", observation.getViewCount());
    values.put("cumulativeViewCount", observation.getCumulativeViewCount());
    return values;
  }

  private static Map<String, Number> ratingValues(AlcoholRatingObservation observation) {
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("deltaRatingCount", observation.getDeltaRatingCount());
    values.put("deltaRatingSum", observation.getDeltaRatingSum());
    values.put("ratingCount", observation.getRatingCount());
    values.put("ratingSum", observation.getRatingSum());
    values.put("averageRating", observation.averageRating());
    return values;
  }

  private static Map<String, Number> pickValues(AlcoholPickObservation observation) {
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("deltaPickCount", observation.getDeltaPickCount());
    values.put("pickCount", observation.getPickCount());
    values.put("unpickCount", observation.getUnpickCount());
    return values;
  }

  private static Map<String, Number> engagementValues(AlcoholEngagementObservation observation) {
    Map<String, Number> values = new LinkedHashMap<>();
    values.put("deltaReviewCount", observation.getDeltaReviewCount());
    values.put("deltaLikeCount", observation.getDeltaLikeCount());
    values.put("deltaDislikeCount", observation.getDeltaDislikeCount());
    values.put("deltaReplyCount", observation.getDeltaReplyCount());
    values.put("reviewCount", observation.getReviewCount());
    values.put("likeCount", observation.getLikeCount());
    values.put("dislikeCount", observation.getDislikeCount());
    values.put("replyCount", observation.getReplyCount());
    return values;
  }

  private static TimeSeriesDescriptor descriptor(
      String key, String label, TimeSeriesUnit unit, TimeSeriesFill fill) {
    return new TimeSeriesDescriptor(key, label, unit, fill);
  }

  private static <T> Optional<T> latest(List<T> rows) {
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(rows.getLast());
  }

  private static <T> long sumLong(List<T> rows, Function<T, Long> getter) {
    long sum = 0L;
    for (T row : rows) {
      sum += nz(getter.apply(row));
    }
    return sum;
  }

  private static <T> BigDecimal sumDecimal(List<T> rows, Function<T, BigDecimal> getter) {
    BigDecimal sum = BigDecimal.ZERO;
    for (T row : rows) {
      BigDecimal value = getter.apply(row);
      if (value != null) {
        sum = sum.add(value);
      }
    }
    return sum;
  }

  private static long nz(Long value) {
    return value == null ? 0L : value;
  }

  private static BigDecimal averageRating(Long ratingCount, BigDecimal ratingSum) {
    if (ratingCount == null || ratingCount == 0L || ratingSum == null) {
      return null;
    }
    return ratingSum.divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
  }
}
