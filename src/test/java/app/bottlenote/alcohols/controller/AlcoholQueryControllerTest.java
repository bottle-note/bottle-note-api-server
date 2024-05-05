package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("알코올(위스키) 쿼리 컨트롤러 테스트")
@WebMvcTest(AlcoholQueryController.class)
@WithMockUser
class AlcoholQueryControllerTest {
	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private AlcoholQueryService alcoholQueryService;

	@DisplayName("술 리스트를 조회할 수 있다.")
	@Test
	void test_case_1() throws Exception {
		// given
		Long userId = null;
		AlcoholSearchRequest request = getRequest();
		PageResponse<AlcoholSearchResponse> response = getResponse();

		// when
		when(alcoholQueryService.searchAlcohols(request, userId)).thenReturn(response);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/alcohols/search")
				.param("keyword", "glen")
				.param("category", "SINGLE_MOLT")
				.param("regionId", "1")
				.param("sortType", "REVIEW")
				.param("sortOrder", "DESC")
				.param("cursor", "0")
				.param("pageSize", "3")
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andDo(print());


		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.totalCount").value(5));
		resultActions.andExpect(jsonPath("$.data.alcohols.size()").value(3));

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
			.picked(false)
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
			.picked(false)
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
