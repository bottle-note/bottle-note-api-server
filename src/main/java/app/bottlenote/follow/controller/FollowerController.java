package app.bottlenote.follow.controller;

import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.service.FollowerService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/follower")
@RequiredArgsConstructor
public class FollowerController {


	private final FollowerService followerService;

	@GetMapping("/{userId}")
	public ResponseEntity<GlobalResponse> findFollowerList(@PathVariable Long userId, @ModelAttribute FollowPageableRequest pageableRequest) {

		PageResponse<FollowSearchResponse> pageResponse = followerService.findFollowerList(userId, pageableRequest);

		return ResponseEntity.ok(
			GlobalResponse.success(
				pageResponse.content(),
				MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable())
			)
		);
	}
}
