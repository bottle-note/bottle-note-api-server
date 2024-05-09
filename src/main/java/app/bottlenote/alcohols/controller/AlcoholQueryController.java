package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.global.security.SecurityUtil.getCurrentUserId;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/alcohols")
public class AlcoholQueryController {

	private final AlcoholQueryService alcoholQueryService;


	/**
	 * 위스키를 검색하는 API 입니다.
	 * 사용자가 있을 경우 좋아요 여부도 함께 전달합니다.
	 *
	 * @param request the request
	 * @return the response entity
	 */
	@GetMapping("/search")
	public ResponseEntity<GlobalResponse> searchAlcohols(
		@ModelAttribute @Valid AlcoholSearchRequest request
	) {
		Long id = getCurrentUserId();

		PageResponse<AlcoholSearchResponse> pageResponse = alcoholQueryService.searchAlcohols(request, id);

		return ResponseEntity.ok(GlobalResponse
			.success(
				pageResponse.content(),
				MetaService.createMetaInfo()
					.add("searchParameters", request)
					.add("pageable", pageResponse.cursorPageable())
			)
		);
	}
}
