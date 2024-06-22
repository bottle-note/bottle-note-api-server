package app.bottlenote.review.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.repository.ReviewImageRepository;
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
class ReviewImageSupportServiceTest {

	@Mock
	private ReviewImageRepository reviewImageRepository;

	@InjectMocks
	private ReviewImageSupportService reviewImageSupportService;

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

		reviewImageSupportService.saveImages(reviewImageInfoList, review);
		verify(reviewImageRepository, times(1)).saveAll(anyList());
	}


	@Test
	@DisplayName("매개변수로 빈 리스트가 전달되면 메서드가 종료된다.")
	void validate__image() {
		List<ReviewImageInfo> emptyList = Collections.EMPTY_LIST;

		reviewImageSupportService.saveImages(emptyList, review);
		verify(reviewImageRepository, never()).saveAll(anyList());
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

		Assertions.assertThrows(ReviewException.class, () -> reviewImageSupportService.saveImages(wrongReviewImages, review));
	}

}
