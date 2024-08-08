package app.bottlenote.support.help.controller;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.help.dto.request.HelpRegisterRequest;
import app.bottlenote.support.help.service.HelpService;
import app.bottlenote.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/help")
@RequiredArgsConstructor
public class HelpCommandController {

	private final HelpService helpService;

	@PostMapping
	public ResponseEntity<?> registerHelp(@RequestBody HelpRegisterRequest helpRegisterRequest) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().
			orElseThrow(() -> new UserException(USER_NOT_FOUND));

		return GlobalResponse.ok(helpService.registerHelp(helpRegisterRequest, currentUserId));
	}

}
