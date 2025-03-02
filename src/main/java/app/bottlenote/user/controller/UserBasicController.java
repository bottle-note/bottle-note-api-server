package app.bottlenote.user.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.request.ProfileImageChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.dto.response.WithdrawUserResultResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.DefaultUserFacade;
import app.bottlenote.user.service.UserBasicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserBasicController {

	private final UserBasicService userBasicService;
	private final DefaultUserFacade userFacade;

	@PatchMapping("/nickname")
	public ResponseEntity<?> nicknameChange(@RequestBody @Valid NicknameChangeRequest nicknameChangeRequest) {

		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		NicknameChangeResponse response = userBasicService.nicknameChange(userId, nicknameChangeRequest);
		return GlobalResponse.ok(response);
	}

	@PatchMapping("/profile-image")
	public ResponseEntity<?> profileImageChange(@RequestBody ProfileImageChangeRequest request) {

		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		ProfileImageChangeResponse response = userBasicService.profileImageChange(userId, request.viewUrl());

		return GlobalResponse.ok(response);
	}

	@DeleteMapping
	public ResponseEntity<?> withdrawUser() {

		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		WithdrawUserResultResponse response = userBasicService.withdrawUser(userId);

		return GlobalResponse.ok(response);
	}

	@GetMapping("/current")
	public ResponseEntity<?> getCurrentUser() {
		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(REQUIRED_USER_ID));
		return GlobalResponse.ok(userFacade.getUserProfileInfo(userId));
	}
}
