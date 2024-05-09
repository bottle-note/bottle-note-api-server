package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
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

		// 파라미터의 닉네임 존재하면 == 중복일때, bad request
		// 파라미터의 닉네임이 존재하지않으면 update 후 ok
		if (userCommandService.isExistNickname(nicknameChangeRequest.nickName())) {
			return ResponseEntity.badRequest()
				.body(GlobalResponse.fail("이미 존재하는 닉네임입니다."));
		}



		String updatedNickname = userCommandService.nicknameChange(nicknameChangeRequest);

		return ResponseEntity.ok(GlobalResponse.success(updatedNickname));

	}


}
