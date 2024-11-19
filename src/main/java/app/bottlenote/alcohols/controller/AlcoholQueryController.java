package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.domain.constant.AlcoholType;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.alcohols.service.AlcoholReferenceService;
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
	private final AlcoholReferenceService alcoholReferenceService;

	@GetMapping("/search")
	public ResponseEntity<?> searchAlcohols(
		@ModelAttribute @Valid AlcoholSearchRequest request
	) {
		Long id = getUserIdByContext().orElse(-1L);

		PageResponse<AlcoholSearchResponse> pageResponse = alcoholQueryService.searchAlcohols(request, id);

		return GlobalResponse.ok(
			pageResponse.content(),
			MetaService.createMetaInfo()
				.add("searchParameters", request)
				.add("pageable", pageResponse.cursorPageable())
		);
	}

	@GetMapping("/{alcoholId}")
	public ResponseEntity<?> findAlcoholDetailById(@PathVariable Long alcoholId) {
		Long id = getUserIdByContext().orElse(-1L);
		return ResponseEntity.ok(
			GlobalResponse.success(alcoholQueryService.findAlcoholDetailById(alcoholId, id)));
	}

	@GetMapping("/categories")
	public ResponseEntity<?> getAlcoholCategory(
		@RequestParam(required = false, defaultValue = "WHISKY") AlcoholType type
	) {
		return GlobalResponse.ok(alcoholReferenceService.getAlcoholCategory(type));
	}
}
