package app.bottlenote.alcohols.controller;

import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.core.structure.Pair;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;

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
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false, defaultValue = "20") Integer size,
			@RequestParam(required = false, defaultValue = "0") Long cursor
	) {
		Long userId = SecurityContextUtil.getUserIdByContext().orElseThrow(
				() -> new UserException(UserExceptionCode.REQUIRED_USER_ID)
		);
		Pair<Long, PageResponse<List<AlcoholDetailItem>>> pair = alcoholQueryService.getStandardExplore(userId, keyword, cursor, size);

		// Map을 사용하여 응답 데이터 구성
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("totalCount", pair.first());
		responseMap.put("items", pair.second().content());

		return GlobalResponse.ok(
				responseMap,
				createMetaInfo().add("pageable", pair.second().cursorPageable())
		);
	}
}
