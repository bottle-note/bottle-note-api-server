package app.bottlenote.rating.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.service.RatingCommandService;
import app.bottlenote.rating.service.RatingQueryService;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	public ResponseEntity<?> getRatingList() {
		return ResponseEntity.ok(
			GlobalResponse.success(
				queryService.getRatingList()
			)
		);
	}
}
