package app.bottlenote.docs.alcohols;

import app.bottlenote.alcohols.controller.AlcoholQueryController;
import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("alcohol 컨트롤러 RestDocs용 테스트")
class RestAlcoholQueryControllerTest extends AbstractRestDocs {

	private final AlcoholQueryService alcoholQueryService = mock(AlcoholQueryService.class);

	@Override
	protected Object initController() {
		return new AlcoholQueryController(alcoholQueryService);
	}

	@DisplayName("술 리스트를 조회할 수 있다.")
	@Test
	void document_test() throws Exception {
		// given
		Long userId = 1L;
		AlcoholSearchRequest request = getRequest();
		PageResponse<AlcoholSearchResponse> response = getResponse();

		// when
		when(alcoholQueryService.searchAlcohols(request, userId)).thenReturn(response);

		// then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/alcohols/search")
				.param("keyword", "glen")
				.param("category", "SINGLE_MOLT")
				.param("regionId", "1")
				.param("sortType", "REVIEW")
				.param("sortOrder", "DESC")
				.param("cursor", "0")
				.param("pageSize", "3")
			)
			.andExpect(status().isOk())
			.andDo(
				document("alcohols/search",
					queryParameters(
						parameterWithName("keyword").optional().description("검색어"),
						parameterWithName("category").optional().description("카테고리 (category API 참조)"),
						parameterWithName("regionId").optional().description("지역 ID (region API 참조)"),
						parameterWithName("sortType").optional().description("정렬 타입(해당 문서 하단 enum 참조)"),
						parameterWithName("sortOrder").optional().description("정렬 순서(해당 문서 하단 enum 참조)"),
						parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
						parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈")
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.totalCount").description("전체 술 리스트의 크기"),
						fieldWithPath("data.alcohols[].alcoholId").description("술 ID"),
						fieldWithPath("data.alcohols[].korName").description("술 한글 이름"),
						fieldWithPath("data.alcohols[].engName").description("술 영문 이름"),
						fieldWithPath("data.alcohols[].korCategoryName").description("술 한글 카테고리 이름"),
						fieldWithPath("data.alcohols[].engCategoryName").description("술 영문 카테고리 이름"),
						fieldWithPath("data.alcohols[].imageUrl").description("술 이미지 URL"),
						fieldWithPath("data.alcohols[].rating").description("술 평점"),
						fieldWithPath("data.alcohols[].ratingCount").description("술 평점 개수"),
						fieldWithPath("data.alcohols[].reviewCount").description("술 리뷰 개수"),
						fieldWithPath("data.alcohols[].pickCount").description("술 찜 개수"),
						fieldWithPath("data.alcohols[].picked").description("술 찜 여부"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored(),
						fieldWithPath("meta.pageable").description("페이징 정보"),
						fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
						fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
						fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
						fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부"),
						fieldWithPath("meta.searchParameters.keyword").description("검색 시 사용 한 검색어"),
						fieldWithPath("meta.searchParameters.category").description("검색 시 사용 한 카테고리"),
						fieldWithPath("meta.searchParameters.regionId").description("검색 시 사용 한 지역 ID"),
						fieldWithPath("meta.searchParameters.sortType").description("검색 시 사용 한 정렬 타입"),
						fieldWithPath("meta.searchParameters.sortOrder").description("검색 시 사용 한 정렬 순서"),
						fieldWithPath("meta.searchParameters.cursor").description("검색 시 사용 한 커서"),
						fieldWithPath("meta.searchParameters.pageSize").description("검색 시 사용 한 페이지 사이즈")

					)
				)
			);

	}

	private AlcoholSearchRequest getRequest() {
		return AlcoholSearchRequest.builder()
			.keyword("glen")
			.category("SINGLE_MOLT")
			.regionId(1L)
			.sortType(SearchSortType.REVIEW)
			.sortOrder(SortOrder.DESC)
			.cursor(0L)
			.pageSize(3L)
			.build();
	}

	private PageResponse<AlcoholSearchResponse> getResponse() {

		AlcoholsSearchDetail detail_1 = AlcoholsSearchDetail.builder()
			.alcoholId(5L)
			.korName("아녹 24년")
			.engName("anCnoc 24-year-old")
			.korCategoryName("싱글 몰트")
			.engCategoryName("Single Malt")
			.imageUrl("https://static.whiskybase.com/storage/whiskies/6/6/989/270671-big.jpg")
			.rating(4.5)
			.ratingCount(1L)
			.reviewCount(0L)
			.pickCount(1L)
			.picked(false)
			.build();

		AlcoholsSearchDetail detail_2 = AlcoholsSearchDetail.builder()
			.alcoholId(1L)
			.korName("글래스고 1770 싱글몰트 스카치 위스키")
			.engName("1770 Glasgow Single Malt")
			.korCategoryName("싱글 몰트")
			.engCategoryName("Single Malt")
			.imageUrl("https://static.whiskybase.com/storage/whiskies/2/0/8916/404538-big.jpg")
			.rating(3.5)
			.ratingCount(3L)
			.reviewCount(1L)
			.pickCount(1L)
			.picked(true)
			.build();

		AlcoholsSearchDetail detail_3 = AlcoholsSearchDetail.builder()
			.alcoholId(2L)
			.korName("글래스고 1770 싱글몰트 스카치 위스키")
			.engName("1770 Glasgow Single Malt")
			.korCategoryName("싱글 몰트")
			.engCategoryName("Single Malt")
			.imageUrl("https://static.whiskybase.com/storage/whiskies/2/0/8888/404535-big.jpg")
			.rating(3.5)
			.ratingCount(1L)
			.reviewCount(0L)
			.pickCount(1L)
			.picked(true)
			.build();


		Long totalCount = 5L;
		List<AlcoholsSearchDetail> details = List.of(detail_1, detail_2, detail_3);
		CursorPageable cursorPageable = CursorPageable.builder()
			.currentCursor(0L)
			.cursor(4L)
			.pageSize(3L)
			.hasNext(true)
			.build();
		AlcoholSearchResponse response = AlcoholSearchResponse.of(totalCount, details);
		return PageResponse.of(response, cursorPageable);
	}

}
