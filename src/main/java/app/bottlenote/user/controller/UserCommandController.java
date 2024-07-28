package app.bottlenote.user.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.dto.response.UserResultResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserCommandController {

	private final UserCommandService userCommandService;

	@PatchMapping("/nickname")
	public ResponseEntity<GlobalResponse> nicknameChange(@RequestBody @Valid NicknameChangeRequest nicknameChangeRequest) {


		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));


		NicknameChangeResponse response = userCommandService.nicknameChange(userId, nicknameChangeRequest);
		return ResponseEntity.ok(GlobalResponse.success(response));
	}

	// 유저 프로필 이미지 변경
	@PatchMapping("/profile-image")
	public ResponseEntity<GlobalResponse> profileImageChange(@RequestBody String viewUrl) {

		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));

		ProfileImageChangeResponse response = userCommandService.profileImageChange(userId, viewUrl);

		return ResponseEntity.ok(GlobalResponse.success(response));
	}

	@DeleteMapping()
	public ResponseEntity<GlobalResponse> withdrawUser() {

		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));

		UserResultResponse response = userCommandService.withdrawUser(userId);

		return ResponseEntity.ok(GlobalResponse.success(response));
	}
}
