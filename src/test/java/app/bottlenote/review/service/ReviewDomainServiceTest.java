package app.bottlenote.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewDomainService;
import app.bottlenote.review.domain.ReviewModifyVO;
import app.bottlenote.review.domain.ReviewTastingTag;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.exception.ReviewException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("리뷰 도메인 서비스 테스트")
class ReviewDomainServiceTest {

	private ReviewDomainService reviewDomainService;

	private Review review;

	private ReviewModifyRequest reviewModifyRequest;

	@BeforeEach
	void setUp() {
		reviewDomainService = new ReviewDomainService();

		reviewModifyRequest = new ReviewModifyRequest(
			"그저 그래요",
			ReviewStatus.PUBLIC,
			BigDecimal.valueOf(10000L),
			SizeType.GLASS,
			List.of("달콤한 향", "스파이시한 맛"),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩")

		);

		review = Review.builder().build();

	}

	@Test
	@DisplayName("ReviewModifyRequest를 ReviewModifyVo로 변환한다.")
	void transfer_to_review_modify_vo() {

		ReviewModifyVO result = reviewDomainService.createValidatedReview(reviewModifyRequest);

		assertEquals(result.getReviewStatus(), reviewModifyRequest.status());
	}

	@Test
	@DisplayName("ReviewModifyRequest의 테이스팅 태그를 ReviewModifyVo로 변환한다.")
	void transfer_to_review_modify_vo_tasting_tag() {

		Set<ReviewTastingTag> reviewTastingTag = reviewDomainService.createValidatedReviewTastingTag(reviewModifyRequest.tastingTagList(), review);

		assertEquals(reviewTastingTag.size(), reviewModifyRequest.tastingTagList().size());
	}

	@Test
	@DisplayName("테이스팅 태그가 10개를 초과하면 예외가 반환된다.")
	void transfer_to_review_modify_vo_tasting_tag_fail() {

		List<String> tastingTagList = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

		assertThrows(ReviewException.class, () -> reviewDomainService.createValidatedReviewTastingTag(tastingTagList, review));
	}
}
