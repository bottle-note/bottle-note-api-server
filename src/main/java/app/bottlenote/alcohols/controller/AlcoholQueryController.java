package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.meta.MetaInfos;
import app.bottlenote.global.service.meta.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/alcohols")
public class AlcoholQueryController {

	private final AlcoholQueryService alcoholQueryService;

	@GetMapping
	public ResponseEntity<GlobalResponse> searchAlcohols(
		@RequestBody @Valid AlcoholSearchRequest request,
		@RequestHeader(value = "Authorization", required = false) String token
	) {
		Long id = 1L;

		MetaInfos metaInfo = MetaService.createMetaInfo()
			.add("search", request)
			.add("token", token);

		return ResponseEntity.ok(GlobalResponse
			.success(
				alcoholQueryService.searchAlcohols(request, id),
				metaInfo
			)
		);
	}
}
