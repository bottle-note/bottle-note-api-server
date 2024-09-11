package app.bottlenote.support.help.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.service.HelpService;
import app.bottlenote.user.exception.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

@RestController
@RequestMapping("/api/v1/help")
@RequiredArgsConstructor
public class HelpCommandController {

	private final HelpService helpService;

	@PostMapping
	public ResponseEntity<?> registerHelp(@Valid @RequestBody HelpUpsertRequest helpUpsertRequest) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().
			orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		return GlobalResponse.ok(helpService.registerHelp(helpUpsertRequest, currentUserId));
	}

}
