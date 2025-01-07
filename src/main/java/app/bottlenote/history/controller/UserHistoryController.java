package app.bottlenote.history.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.history.service.UserHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class UserHistoryController {

	private final UserHistoryQueryService userHistoryQueryService;

	@GetMapping("/{targetUserId}")
	public ResponseEntity<?> findUserHistoryList(
		@PathVariable Long targetUserId,
		@RequestParam(defaultValue = "0") Integer cursor,
		@RequestParam(defaultValue = "10") Integer pageSize
	) {

		PageResponse<UserHistorySearchResponse> userHistoryList = userHistoryQueryService.findUserHistoryList(targetUserId, cursor, pageSize);
		return GlobalResponse.ok(
			userHistoryList.content(),
			MetaService.createMetaInfo().add("pageable", userHistoryList.cursorPageable())
		);
	}
}
