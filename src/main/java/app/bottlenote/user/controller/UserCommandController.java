package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.service.UserCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

		NicknameChangeResponse response = userCommandService.nicknameChange(nicknameChangeRequest);
		return ResponseEntity.ok(GlobalResponse.success(response));
	}

}
