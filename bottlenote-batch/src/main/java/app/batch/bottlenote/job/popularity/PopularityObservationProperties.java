package app.batch.bottlenote.job.popularity;

import app.bottlenote.alcohols.constant.BucketGranularity;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 인기도 관측 설정.
 *
 * <p>가중치와 정규화 기준은 아직 확정되지 않았다. 축별 시계열이 쌓인 뒤 실제 분포를 보고 정하기로 했으므로, 여기 값은 조정 가능한 출발점이지 결정된 산식이 아니다.
 *
 * <p>정규화는 전역 최댓값을 쓰지 않는다. 같은 버킷 안 다른 주류의 최댓값으로 나누면 자기 값이 그대로인데도 점수가 흔들려 시계열이 무의미해진다.
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "popularity.observation")
public class PopularityObservationProperties {

  private Weights weights = new Weights();
  private References reference = new References();
  private Retention retention = new Retention();

  /**
   * 잘못된 설정으로 조용히 오작동하느니 기동에 실패하는 편이 낫다.
   *
   * <p>가중치 합이 1이 아니면 점수 범위가 무너지고, 정규화 기준이 0 이하면 모든 축이 0점이 된다. 둘 다 데이터가 쌓인 뒤에야 드러나므로 여기서 막는다.
   */
  @PostConstruct
  void verifyOnStartup() {
    List<String> errors = validate();
    if (!errors.isEmpty()) {
      throw new IllegalStateException("인기도 관측 설정이 올바르지 않습니다: " + String.join(", ", errors));
    }
    log.info(
        "인기도 관측 설정 로드. 가중치=[관심 {}, 평가 {}, 선호 {}, 참여 {}], HOUR/WEEK/MONTH 정규화 기준 적용, HOUR 보관 {}일",
        weights.interest,
        weights.rating,
        weights.pick,
        weights.engagement,
        retention.hourDays);
  }

  /** 설정 오류를 조용히 넘기지 않기 위해 기동 시 검증한다. */
  public List<String> validate() {
    List<String> errors = new ArrayList<>();

    BigDecimal total =
        weights.interest.add(weights.rating).add(weights.pick).add(weights.engagement);
    if (total.compareTo(BigDecimal.ONE) != 0) {
      errors.add("가중치 합이 1.0이 아닙니다: " + total);
    }
    checkWeight(weights.interest, "관심도 가중치", errors);
    checkWeight(weights.rating, "평가도 가중치", errors);
    checkWeight(weights.pick, "선호도 가중치", errors);
    checkWeight(weights.engagement, "참여도 가중치", errors);

    if (retention.hourDays < Retention.MINIMUM_HOUR_DAYS) {
      errors.add(
          "HOUR 관측 보관 일수는 " + Retention.MINIMUM_HOUR_DAYS + " 이상이어야 합니다: " + retention.hourDays);
    }

    for (BucketGranularity granularity : BucketGranularity.values()) {
      Reference periodReference = referenceFor(granularity);
      checkReference(periodReference.interest, granularity + " 관심도 정규화 기준", errors);
      checkReference(periodReference.rating, granularity + " 평가도 정규화 기준", errors);
      checkReference(periodReference.pick, granularity + " 선호도 정규화 기준", errors);
      checkReference(periodReference.engagement, granularity + " 참여도 정규화 기준", errors);
    }

    return errors;
  }

  public boolean isValid() {
    return validate().isEmpty();
  }

  public Reference referenceFor(BucketGranularity granularity) {
    return switch (granularity) {
      case HOUR -> reference.hour;
      case WEEK -> reference.week;
      case MONTH -> reference.month;
    };
  }

  private void checkWeight(BigDecimal value, String name, List<String> errors) {
    if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
      errors.add(name + "는 0.0~1.0 사이여야 합니다: " + value);
    }
  }

  private void checkReference(long value, String name, List<String> errors) {
    if (value <= 0) {
      errors.add(name + "는 1 이상이어야 합니다: " + value);
    }
  }

  @Getter
  @Setter
  public static class Weights {
    private BigDecimal interest = new BigDecimal("0.25");
    private BigDecimal rating = new BigDecimal("0.25");
    private BigDecimal pick = new BigDecimal("0.25");
    private BigDecimal engagement = new BigDecimal("0.25");
  }

  @Getter
  @Setter
  public static class References {
    private Reference hour = new Reference();
    private Reference week = new Reference();
    private Reference month = new Reference();
  }

  /**
   * HOUR 원본 보관 기간.
   *
   * <p>정리는 월간 Job이 하므로 실제 보관은 이 값보다 길어질 수 있다. 짧게 줄이면 롤업 검증과 재실행 여유가 사라지므로 하한을 둔다.
   */
  @Getter
  @Setter
  public static class Retention {
    static final int MINIMUM_HOUR_DAYS = 45;

    private int hourDays = MINIMUM_HOUR_DAYS;
  }

  /** 각 축을 0~1로 누르는 분모. 이 값에 도달하면 해당 축은 만점이다. */
  @Getter
  @Setter
  public static class Reference {
    private long interest = 50L;
    private long rating = 100L;
    private long pick = 200L;
    private long engagement = 300L;
  }
}
