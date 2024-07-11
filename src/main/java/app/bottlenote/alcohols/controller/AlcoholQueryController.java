package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.domain.constant.AlcoholType;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/alcohols")
public class AlcoholQueryController {

	private final AlcoholQueryService alcoholQueryService;


	/**
	 * 위스키를 검색하는 API 입니다.
	 * 사용자가 있을 경우 좋아요 여부도 함께 전달합니다.
	 * <p>
	 * 유저 아이디가 존재하지않을때 userId를 -1L 로 조회 :
	 * "isPicked" : false 값으로만 조회됩니다.
	 *
	 * @param request the request
	 * @return the response entity
	 */
	@GetMapping("/search")
	public ResponseEntity<GlobalResponse> searchAlcohols(
		@ModelAttribute @Valid AlcoholSearchRequest request
	) {
		Long id = getUserIdByContext().orElse(-1L);

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

	/**
	 * 위스키의 상세정보 API 입니다.
	 * <p>
	 * 유저 아이디가 존재하지않을때 userId를 -1L 로 조회 :
	 * "myRating": null, "isPicked": false 값으로만 조회됩니다.
	 *
	 * @param alcoholId
	 * @return the response entity
	 */
	@GetMapping("/{alcoholId}")
	public ResponseEntity<GlobalResponse> findAlcoholDetailById(@PathVariable Long alcoholId) {
		Long id = getUserIdByContext().orElse(-1L);
		return ResponseEntity.ok(
			GlobalResponse.success(alcoholQueryService.findAlcoholDetailById(alcoholId, id)));
	}


	@GetMapping("/categories")
	public ResponseEntity<GlobalResponse> getAlcoholCategory(
		@RequestParam(required = false, defaultValue = "WHISKY") AlcoholType type
	) {
		return ResponseEntity.ok(GlobalResponse.success(alcoholQueryService.getAlcoholCategory(type)));
	}
}
