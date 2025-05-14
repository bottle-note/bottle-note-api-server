package app.bottlenote.alcohols.controller;

import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.core.structure.Pair;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.CursorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/alcohols/explore")
public class AlcoholExploreController {

	private final AlcoholQueryService alcoholQueryService;

	/**
	 * 기본 표준 형태의 위스키 둘러보기 API
	 * 인기도, 최신 업데이트, 카테고리 등을 종합적으로 고려한 둘러보기 목록 제공
	 */
	@GetMapping("/standard")
	public ResponseEntity<?> getStandardExplore(
			@RequestParam(required = false) List<String> keyword,
			@RequestParam(required = false, defaultValue = "20") Integer size,
			@RequestParam(required = false, defaultValue = "0") Long cursor
	) {
		Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
		Pair<Long, CursorResponse<AlcoholDetailItem>> pair = alcoholQueryService.getStandardExplore(userId, keyword, cursor, size);
		return GlobalResponse.ok(pair, keyword);
	}
}
