package app.bottlenote.review.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.repository.ReviewTastingTagRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("리뷰 이미지 서포트 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewTastingTagSupportServiceTest {

	@Mock
	private ReviewTastingTagRepository reviewTastingTagRepository;

	@InjectMocks
	private ReviewTastingTagSupportService reviewTastingTagSupportService;

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

		reviewTastingTagSupportService.saveReviewTastingTag(tastingTags, review);
		verify(reviewTastingTagRepository, times(1)).saveAll(any());
	}


	@Test
	@DisplayName("테이스팅 태그로 빈 리스트가 전달되면 메서드가 종료된다.")
	void validate_tasting_tag_list() {
		List<String> emptyList = Collections.EMPTY_LIST;

		reviewTastingTagSupportService.saveReviewTastingTag(emptyList, review);
		verify(reviewTastingTagRepository, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("테이스팅 태그가 10개 이상이면 저장할 수 없다")
	void fail_when_tasting_tag_is_more_than_ten() {
		List<String> wrongTastingTags = List.of(
			"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");

		Assertions.assertThrows(ReviewException.class, () -> reviewTastingTagSupportService.saveReviewTastingTag(wrongTastingTags, review));
	}

}
