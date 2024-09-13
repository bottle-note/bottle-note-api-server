package app.bottlenote.support.help.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.service.HelpService;
import app.bottlenote.user.exception.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@PatchMapping("/{helpId}")
	public ResponseEntity<?> modifyHelp(@Valid @RequestBody HelpUpsertRequest helpUpsertRequest, @PathVariable Long helpId){

		Long currentUserId = SecurityContextUtil.getUserIdByContext().
			orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		return GlobalResponse.ok(helpService.modifyHelp(helpUpsertRequest, currentUserId, helpId));
	}

	@DeleteMapping("/{helpId}")
	public ResponseEntity<?> deleteHelp(@PathVariable Long helpId){

		Long currentUserId = SecurityContextUtil.getUserIdByContext().
			orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		return GlobalResponse.ok(helpService.deleteHelp(helpId, currentUserId));
	}
}
