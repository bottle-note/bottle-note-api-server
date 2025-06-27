package app.bottlenote.support.block.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.block.exception.BlockException;
import app.bottlenote.support.block.service.BlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.support.block.exception.BlockExceptionCode.REQUIRED_USER_ID;

/**
 * 차단 관리 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/blocks")
public class BlockController {

	private final BlockService blockService;

	/**
	 * 사용자 차단
	 */
	@PostMapping("/create")
	public ResponseEntity<?> createBlock(@RequestBody BlockCreateRequest request) {
		Long currentUserId = SecurityContextUtil.getUserIdByContext()
				.orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

		blockService.blockUser(currentUserId, request.blockedUserId());

		return GlobalResponse.ok("차단이 완료되었습니다.");
	}

	/**
	 * 차단 해제
	 */
	@DeleteMapping("/{blockedUserId}")
	public ResponseEntity<?> unblockUser(@PathVariable Long blockedUserId) {
		Long currentUserId = SecurityContextUtil.getUserIdByContext()
				.orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

		blockService.unblockUser(currentUserId, blockedUserId);

		return GlobalResponse.ok("차단이 해제되었습니다.");
	}

	/**
	 * 차단 목록 조회
	 */
	@GetMapping
	public ResponseEntity<?> getBlockedUsers() {
		Long currentUserId = SecurityContextUtil.getUserIdByContext()
				.orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

		return GlobalResponse.ok(blockService.getBlockedUserIds(currentUserId));
	}

	/**
	 * 차단 요청 DTO
	 */
	public record BlockCreateRequest(Long blockedUserId) {
	}
}
