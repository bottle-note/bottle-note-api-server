package app.bottlenote.review.service;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.exception.ReviewException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@DisplayName("[unit] [service] ReviewTastingTagSupport")
@ExtendWith(MockitoExtension.class)
class ReviewTastingTagSupportTest {

	@InjectMocks
	private ReviewTastingTagSupport reviewTastingTagSupport;

	private Review review;

	private List<String> tastingTags;

	@BeforeEach
	void setUp() {
		review = Review.builder().build();

		tastingTags = List.of(
			"xx향", "yy향", "zz향", "aa향", "bb향"
		);
	}

	@Test
	@DisplayName("테이스팅 태그가 정상적으로 저장된다.")
	void success_tasting_tag() {

		reviewTastingTagSupport.saveReviewTastingTag(tastingTags, review);
		assertEquals(tastingTags.size(), review.getReviewTastingTags().size());
	}


	@Test
	@DisplayName("테이스팅 태그로 빈 리스트가 전달되면 메서드가 종료된다.")
	void validate_tasting_tag_list() {

		List<String> emptyList = List.of();

		reviewTastingTagSupport.saveReviewTastingTag(emptyList, review);
		assertEquals(0, review.getReviewTastingTags().size());
	}

	@Test
	@DisplayName("테이스팅 태그가 10개 이상이면 저장할 수 없다")
	void fail_when_tasting_tag_is_more_than_ten() {
		List<String> wrongTastingTags = List.of(
			"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");

		assertThrows(ReviewException.class, () -> reviewTastingTagSupport.saveReviewTastingTag(wrongTastingTags, review));
	}

}
