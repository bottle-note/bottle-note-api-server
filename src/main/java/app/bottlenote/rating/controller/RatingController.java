package app.bottlenote.rating.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.service.RatingCommandService;
import app.bottlenote.rating.service.RatingQueryService;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/rating")
@RestController
public class RatingController {

	private static final Logger log = LoggerFactory.getLogger(RatingController.class);
	private final RatingCommandService commandService;
	private final RatingQueryService queryService;

	public RatingController(RatingCommandService commandService, RatingQueryService queryService) {
		this.commandService = commandService;
		this.queryService = queryService;
	}

	@PostMapping("/register")
	public ResponseEntity<?> registerRatingPoint(
		@RequestBody RatingRegisterRequest request
	) {
		log.info("RatingRegisterRequest: {}", request);
		Long userId = SecurityContextUtil.getUserIdByContext().orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));


		return ResponseEntity.ok(
			GlobalResponse.success(
				commandService.register(request.alcoholId(), userId, request.rating()
				)
			)
		);
	}
}
