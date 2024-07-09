package app.bottlenote.rating.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import app.bottlenote.rating.exception.RatingException;
import app.bottlenote.rating.service.RatingCommandService;
import app.bottlenote.rating.service.RatingQueryService;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static app.bottlenote.rating.exception.RatingExceptionCode.REQUEST_USER_ID;

@Slf4j
@RestController
@RequestMapping("/api/v1/rating")
public class RatingController {

	private final RatingCommandService commandService;
	private final RatingQueryService queryService;

	public RatingController(RatingCommandService commandService, RatingQueryService queryService) {
		this.commandService = commandService;
		this.queryService = queryService;
	}

	@PostMapping("/register")
	public ResponseEntity<?> registerRatingPoint(
		@RequestBody @Valid RatingRegisterRequest request
	) {
		log.info("RatingRegisterRequest: {}", request);

		Long userId = SecurityContextUtil.getUserIdByContext()
			.orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));
		RatingPoint ratingPoint = RatingPoint.of(request.rating());

		return ResponseEntity.ok(
			GlobalResponse.success(
				commandService.register(request.alcoholId(), userId, ratingPoint
				)
			)
		);
	}

	@GetMapping
	public ResponseEntity<?> fetchRatingList(
		@ModelAttribute RatingListFetchRequest request
	) {
		Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
		var response = queryService.fetchRatingList(request, userId);
		return ResponseEntity.ok(
			GlobalResponse.success(
				response.content(),
				MetaService.createMetaInfo()
					.add("searchParameters", request)
					.add("pageable", response.cursorPageable())
			)
		);
	}

	@GetMapping("/{alcoholId}")
	public ResponseEntity<?> fetchRatingPoint(@PathVariable Long alcoholId) {

		Long userId = SecurityContextUtil.getUserIdByContext()
			.orElseThrow(() -> new RatingException(REQUEST_USER_ID));

		UserRatingResponse response = queryService.fetchUserRating(alcoholId, userId);

		return ResponseEntity.ok(GlobalResponse.success(response));
	}

}
