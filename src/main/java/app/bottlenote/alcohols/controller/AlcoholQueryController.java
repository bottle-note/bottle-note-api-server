package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	 * @param token   the token
	 * @return the response entity
	 */
	@GetMapping
	public ResponseEntity<GlobalResponse> searchAlcohols(
		@ModelAttribute @Valid AlcoholSearchRequest request,
		@RequestHeader(value = "Authorization", required = false) String token
	) {
		Long id = 1L; //todo: token 에서 id 추출 로직으로 변경 필요

		PageResponse<?> pageResponse = alcoholQueryService.searchAlcohols(request, id);

		return ResponseEntity.ok(GlobalResponse
			.success(
				//details.getContent(),
				pageResponse.content(),
				MetaService.createMetaInfo()
					.add("searchParameters", request)
					.add("pageable", pageResponse.cursorPageable())
					.add("token", token) //삭제 예정
			)
		);
	}
}
