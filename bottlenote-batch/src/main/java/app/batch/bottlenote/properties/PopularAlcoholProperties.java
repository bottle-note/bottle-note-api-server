package app.batch.bottlenote.properties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 인기 주류 설정을 위한 Properties 클래스
 * popular.yaml 파일의 설정값들을 매핑합니다.
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "popular")
public class PopularAlcoholProperties {

	private Ranking ranking = new Ranking();
	private Weights weights = new Weights();
	private Scoring scoring = new Scoring();
	private Thresholds thresholds = new Thresholds();

	public void init() {
		log.info("=".repeat(80));
		log.info("🚀 인기 주류 설정 로드 완료");
		log.info("=".repeat(80));
		log.info("📊 랭킹 설정:");
		log.info("  └─ 상위 선정 개수: {} 위", ranking.limit);
		log.info("");
		log.info("⚖️  가중치 설정:");
		log.info("  ├─ 리뷰 점수: {}% ({})", weights.reviewScore.multiply(BigDecimal.valueOf(100)).intValue(), weights.reviewScore);
		log.info("  ├─ 평점 점수: {}% ({})", weights.ratingScore.multiply(BigDecimal.valueOf(100)).intValue(), weights.ratingScore);
		log.info("  └─ 찜하기 점수: {}% ({})", weights.pickScore.multiply(BigDecimal.valueOf(100)).intValue(), weights.pickScore);
		log.info("");
		log.info("🎯 점수 계산 설정:");
		log.info("  ├─ 리뷰 점수:");
		log.info("  │   ├─ 개수 가중치: {}% ({})", scoring.review.countWeight.multiply(BigDecimal.valueOf(100)).intValue(), scoring.review.countWeight);
		log.info("  │   ├─ 조회수 가중치: {}% ({})", scoring.review.viewWeight.multiply(BigDecimal.valueOf(100)).intValue(), scoring.review.viewWeight);
		log.info("  │   ├─ 좋아요 가중치: {}% ({})", scoring.review.likeWeight.multiply(BigDecimal.valueOf(100)).intValue(), scoring.review.likeWeight);
		log.info("  │   └─ 시간 감쇠: {} 일", scoring.review.timeDecayDays);
		log.info("  └─ 평점 점수:");
		log.info("      ├─ 일관성 가중치: {}% ({})", scoring.rating.consistencyWeight.multiply(BigDecimal.valueOf(100)).intValue(), scoring.rating.consistencyWeight);
		log.info("      └─ 평균값 가중치: {}% ({})", scoring.rating.averageWeight.multiply(BigDecimal.valueOf(100)).intValue(), scoring.rating.averageWeight);
		log.info("");
		log.info("🔗 최소 기준값:");
		log.info("  ├─ 최소 인기도 점수: {}", thresholds.minimumScore);
		log.info("  ├─ 최소 리뷰 개수: {} 개", thresholds.minimumReviews);
		log.info("  └─ 최소 평점 개수: {} 개", thresholds.minimumRatings);
		log.info("");

		// 설정 검증 실행
		List<String> validationErrors = validateConfiguration();
		if (validationErrors.isEmpty()) {
			log.info("✅ 설정 유효성 검증: 성공");
		} else {
			log.error("❌ 설정 유효성 검증: 실패");
			validationErrors.forEach(error -> log.error("  └─ {}", error));
		}
		log.info("=".repeat(80));
	}

	/**
	 * 설정값 수동 검증 (Jakarta Validation 대신 사용)
	 *
	 * @return 검증 오류 메시지 리스트
	 */
	private List<String> validateConfiguration() {
		List<String> errors = new ArrayList<>();

		// 랭킹 제한 검증
		if (ranking.limit < 1 || ranking.limit > 1000) {
			errors.add("랭킹 제한은 1~1000 사이여야 합니다: " + ranking.limit);
		}

		// 전체 가중치 검증
		BigDecimal totalWeight = weights.reviewScore.add(weights.ratingScore).add(weights.pickScore);
		if (totalWeight.compareTo(BigDecimal.ONE) != 0) {
			errors.add("전체 가중치 합이 1.0이 아닙니다: " + totalWeight);
		}

		// 개별 가중치 범위 검증
		validateWeightRange(weights.reviewScore, "리뷰 점수 가중치", errors);
		validateWeightRange(weights.ratingScore, "평점 점수 가중치", errors);
		validateWeightRange(weights.pickScore, "찜하기 점수 가중치", errors);

		// 리뷰 점수 가중치 검증
		BigDecimal reviewWeightTotal = scoring.review.countWeight
				.add(scoring.review.viewWeight)
				.add(scoring.review.likeWeight);
		if (reviewWeightTotal.compareTo(BigDecimal.ONE) != 0) {
			errors.add("리뷰 가중치 합이 1.0이 아닙니다: " + reviewWeightTotal);
		}

		validateWeightRange(scoring.review.countWeight, "리뷰 개수 가중치", errors);
		validateWeightRange(scoring.review.viewWeight, "조회수 가중치", errors);
		validateWeightRange(scoring.review.likeWeight, "좋아요 가중치", errors);

		// 평점 점수 가중치 검증
		BigDecimal ratingWeightTotal = scoring.rating.consistencyWeight
				.add(scoring.rating.averageWeight);
		if (ratingWeightTotal.compareTo(BigDecimal.ONE) != 0) {
			errors.add("평점 가중치 합이 1.0이 아닙니다: " + ratingWeightTotal);
		}

		validateWeightRange(scoring.rating.consistencyWeight, "평점 일관성 가중치", errors);
		validateWeightRange(scoring.rating.averageWeight, "평점 평균 가중치", errors);

		// 시간 감쇠 일수 검증
		if (scoring.review.timeDecayDays < 1 || scoring.review.timeDecayDays > 365) {
			errors.add("시간 감쇠 일수는 1~365 사이여야 합니다: " + scoring.review.timeDecayDays);
		}

		// 최소값 검증
		validateWeightRange(thresholds.minimumScore, "최소 인기도 점수", errors);

		if (thresholds.minimumReviews < 0) {
			errors.add("최소 리뷰 개수는 0 이상이어야 합니다: " + thresholds.minimumReviews);
		}

		if (thresholds.minimumRatings < 0) {
			errors.add("최소 평점 개수는 0 이상이어야 합니다: " + thresholds.minimumRatings);
		}

		return errors;
	}

	/**
	 * 가중치 범위 검증 헬퍼 메서드
	 */
	private void validateWeightRange(BigDecimal weight, String fieldName, List<String> errors) {
		if (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.ONE) > 0) {
			errors.add(fieldName + "는 0.0~1.0 사이여야 합니다: " + weight);
		}
	}

	/**
	 * 설정이 유효한지 확인
	 *
	 * @return 유효성 검증 결과
	 */
	public boolean isValid() {
		return validateConfiguration().isEmpty();
	}

	/**
	 * 디버깅용 toString
	 */
	@Override
	public String toString() {
		return String.format(
				"PopularAlcoholProperties{ranking.limit=%d, weights=[review=%.2f, rating=%.2f, pick=%.2f], " +
						"thresholds=[minScore=%.3f, minReviews=%d, minRatings=%d]}",
				ranking.limit,
				weights.reviewScore.doubleValue(),
				weights.ratingScore.doubleValue(),
				weights.pickScore.doubleValue(),
				thresholds.minimumScore.doubleValue(),
				thresholds.minimumReviews,
				thresholds.minimumRatings
		);
	}

	@Getter
	@Setter
	public static class Ranking {
		private int limit = 50;
	}

	@Getter
	@Setter
	public static class Weights {
		private BigDecimal reviewScore = BigDecimal.valueOf(0.4);
		private BigDecimal ratingScore = BigDecimal.valueOf(0.3);
		private BigDecimal pickScore = BigDecimal.valueOf(0.3);
	}

	@Getter
	@Setter
	public static class Scoring {
		private Review review = new Review();
		private Rating rating = new Rating();

		@Getter
		@Setter
		public static class Review {
			private BigDecimal countWeight = BigDecimal.valueOf(0.4);
			private BigDecimal viewWeight = BigDecimal.valueOf(0.3);
			private BigDecimal likeWeight = BigDecimal.valueOf(0.3);
			private int timeDecayDays = 30;
		}

		@Getter
		@Setter
		public static class Rating {
			private BigDecimal consistencyWeight = BigDecimal.valueOf(0.4);
			private BigDecimal averageWeight = BigDecimal.valueOf(0.6);
		}
	}

	@Getter
	@Setter
	public static class Thresholds {
		private BigDecimal minimumScore = BigDecimal.valueOf(0.01);
		private int minimumReviews = 1;
		private int minimumRatings = 1;
	}
}
