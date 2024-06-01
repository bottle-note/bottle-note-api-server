package app.bottlenote.review.controller;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewCreateRequestTest {

	@Test
	@DisplayName("reviewCreateRequest의 validation에 통과한다.")
	void review_create_test_success() {
		assertDoesNotThrow(() -> new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요",
			null,
			new BigDecimal("10.0"),
			null,
			List.of(),
			List.of("tag1", "tag2", "tag3")
		));
	}

	@Test
	@DisplayName("테이스팅 태그의 길이가 12자 이상이면 예외가 발생한다.")
	void invalid_tasting_tag_length() {
		ReviewException exception = assertThrows(ReviewException.class, () -> new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요.",
			null,
			new BigDecimal("10.0"),
			null,
			List.of(),
			List.of("tag1", "처음먹어봐서 무슨맛인지 모르겠는 맛")
		));
		assertEquals(exception.getMessage(), ReviewExceptionCode.INVALID_TASTING_TAG_LENGTH.getMessage());
	}

	@Test
	@DisplayName("테이스팅 태그가 10개를 초과하면 예외가 발생한다.")
	void invalid_tasting_tag_list_size() {
		ReviewException exception = assertThrows(ReviewException.class, () -> new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요.",
			null,
			new BigDecimal("10.0"),
			null,
			List.of(),
			List.of("맛1", "맛2", "맛3", "맛4", "맛5", "맛6", "맛7", "맛8", "맛9", "맛10", "맛11")
		));
		assertEquals(exception.getMessage(), ReviewExceptionCode.INVALID_TASTING_TAG_LIST_SIZE.getMessage());
	}

	@Test
	@DisplayName("중복된 테이스팅 태그는 중복을 제거한다.")
	void remove_duplicate_tasting_tag() {
		ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요.",
			null,
			new BigDecimal("10.0"),
			null,
			List.of(),
			List.of("달콤", "달콤", "시트러스향", "사과 향")
		);
		assertEquals(3, reviewCreateRequest.tastingTagList().size());
	}

	@Test
	@DisplayName("테이스팅 태그의 중복을 검사할 때, 앞 뒤 공백을 제거 한 뒤 중복을 검사한다.")
	void remove_duplicate_tasting_tag_trim() {
		ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요.",
			null,
			new BigDecimal("10.0"),
			null,
			List.of(),
			List.of("달콤 ", "달콤", " 시트러스향", "시트러스향")
		);
		assertEquals(2, reviewCreateRequest.tastingTagList().size());
	}

	@Test
	@DisplayName("빈 테이스팅 태그를 허용한다.")
	void empty_tasting_tag_list() {
		assertDoesNotThrow(() -> new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요.",
			null,
			new BigDecimal("10.0"),
			null,
			List.of(),
			List.of()
		));
	}
}
