package app.bottlenote.review.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.service.ReviewService;
import app.bottlenote.user.exception.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.global.data.response.GlobalResponse.success;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping
	public ResponseEntity<GlobalResponse> createReview(@RequestBody @Valid ReviewCreateRequest reviewCreateRequest) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().
			orElseThrow(() -> new UserException(USER_NOT_FOUND));

		ReviewCreateResponse createdReview = reviewService.createReview(reviewCreateRequest, currentUserId);
		return ResponseEntity.ok(success(createdReview));
	}

	@GetMapping("/{alcoholId}")
	public ResponseEntity<GlobalResponse> getReviews(@PathVariable Long alcoholId, @ModelAttribute PageableRequest pageableRequest) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

		log.info("currentUserId is : {} \nPageable INFO  : {}", currentUserId, pageableRequest.toString());

		PageResponse<ReviewListResponse> pageResponse = reviewService.getReviews(
			alcoholId,
			pageableRequest,
			currentUserId
		);

		return ResponseEntity.ok(
			success(pageResponse.content(), MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable())
			));
	}

	/**
	 * 위스키의 리뷰 상세를 조회하는 API 입니다.
	 * <p>
	 * 유저 아이디가 존재하지않을때 userId를 -1L 로 조회 :
	 * "isPicked": false 값으로만 조회됩니다.
	 * "isMyReview": false 값으로만 조회됩니다.
	 * "isLikedByMe": false 값으로만 조회됩니다.
	 * "hasReplyByMe": false 값으로만 조회됩니다.
	 *
	 * @param reviewId
	 * @return
	 */
	@GetMapping("/detail/{reviewId}")
	public ResponseEntity<GlobalResponse> getDetailReview(@PathVariable Long reviewId) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

		return ResponseEntity.ok(
			success(reviewService.getDetailReview(reviewId, currentUserId))
		);
	}

	@GetMapping("/me/{alcoholId}")
	public ResponseEntity<GlobalResponse> getMyReviews(@ModelAttribute PageableRequest pageableRequest, @PathVariable Long alcoholId) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElseThrow(
			() -> new UserException(REQUIRED_USER_ID)
		);

		PageResponse<ReviewListResponse> myReviews = reviewService.getMyReviews(pageableRequest, alcoholId, currentUserId);

		return ResponseEntity.ok(
			success(myReviews.content(), MetaService.createMetaInfo().add("pageable", myReviews.cursorPageable()))
		);
	}

	@PatchMapping("/{reviewId}")
	public ResponseEntity<GlobalResponse> modifyReview(@RequestBody @Valid ReviewModifyRequest reviewModifyRequest, @PathVariable Long reviewId) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElseThrow(
			() -> new UserException(REQUIRED_USER_ID)
		);

		return ResponseEntity.ok(
			success(reviewService.modifyReview(reviewModifyRequest, reviewId, currentUserId))
		);
	}

	@DeleteMapping("/{reviewId}")
	public ResponseEntity<GlobalResponse> deleteReview(@PathVariable Long reviewId) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElseThrow(
			() -> new UserException(REQUIRED_USER_ID)
		);

		return ResponseEntity.ok(
			success(reviewService.deleteReview(reviewId, currentUserId))
		);
	}
}
