package app.bottlenote.review.service;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.exception.ReviewException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("리뷰 이미지 서포트 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewImageSupportTest {

	@InjectMocks
	private ReviewImageSupport reviewImageSupport;

	private Review review;

	private List<ReviewImageInfo> reviewImageInfoList;

	@BeforeEach
	void setUp() {
		review = Review.builder().build();

		reviewImageInfoList = List.of(
			new ReviewImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"),
			new ReviewImageInfo(2L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/2"),
			new ReviewImageInfo(3L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3"),
			new ReviewImageInfo(4L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/4"),
			new ReviewImageInfo(5L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/5"));
	}

	@Test
	@DisplayName("이미지가 정상적으로 저장된다.")
	void success_images_save() {

		reviewImageSupport.saveImages(reviewImageInfoList, review);

		Assertions.assertEquals(reviewImageInfoList.size(), review.getReviewImages().size());
	}


	@Test
	@DisplayName("매개변수로 빈 리스트가 전달되면 메서드가 종료된다.")
	void validate__image() {

		List<ReviewImageInfo> empytyList = List.of();
		
		reviewImageSupport.saveImages(empytyList, review);

		Assertions.assertEquals(0, review.getReviewImages().size());

	}

	@Test
	@DisplayName("이미지가 다섯장 이상이면 저장할 수 없다")
	void fail_when_image_is_more_than_five() {
		List<ReviewImageInfo> wrongReviewImages = List.of(
			new ReviewImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"),
			new ReviewImageInfo(2L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/2"),
			new ReviewImageInfo(3L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3"),
			new ReviewImageInfo(4L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/4"),
			new ReviewImageInfo(5L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/5"),
			new ReviewImageInfo(6L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/6"));

		Assertions.assertThrows(ReviewException.class, () -> reviewImageSupport.saveImages(wrongReviewImages, review));
	}

}
