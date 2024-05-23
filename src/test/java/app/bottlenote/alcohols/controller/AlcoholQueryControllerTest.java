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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser()
@DisplayName("알코올(위스키) 쿼리 컨트롤러 테스트")
@WebMvcTest(AlcoholQueryController.class)
class AlcoholQueryControllerTest {
	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private AlcoholQueryService alcoholQueryService;

	static Stream<Arguments> testCase1Provider() {
		return Stream.of(
			Arguments.of("모든 요청 파라미터가 존재할 때.",
				AlcoholSearchRequest.builder()
					.keyword("glen")
					.category("SINGLE_MOLT")
					.regionId(1L)
					.sortType(SearchSortType.REVIEW)
					.sortOrder(SortOrder.DESC)
					.cursor(0L)
					.pageSize(3L)
					.build()
			), Arguments.of("키워드가 없을 때.",
				AlcoholSearchRequest.builder()
					.keyword("")
					.category("SINGLE_MOLT")
					.regionId(1L)
					.sortType(SearchSortType.REVIEW)
					.sortOrder(SortOrder.DESC)
					.cursor(0L)
					.pageSize(3L)
					.build()
			), Arguments.of("카테고리도 없을 때.",
				AlcoholSearchRequest.builder()
					.keyword("")
					.category("")
					.regionId(1L)
					.sortType(SearchSortType.REVIEW)
					.sortOrder(SortOrder.DESC)
					.cursor(0L)
					.pageSize(3L)
					.build()
			), Arguments.of("지역 아이디도 없을 때.",
				AlcoholSearchRequest.builder()
					.keyword("")
					.category("")
					.regionId(null)
					.sortType(SearchSortType.REVIEW)
					.sortOrder(SortOrder.DESC)
					.cursor(0L)
					.pageSize(3L)
					.build()
			), Arguments.of("정렬 정보도 없을 때.",
				AlcoholSearchRequest.builder()
					.keyword("")
					.category("")
					.regionId(null)
					.sortType(null)
					.sortOrder(null)
					.cursor(0L)
					.pageSize(3L)
					.build()
			), Arguments.of("페이지 정보도도 없을 때.",
				AlcoholSearchRequest.builder()
					.keyword("")
					.category("")
					.regionId(null)
					.sortType(null)
					.sortOrder(null)
					.cursor(null)
					.pageSize(null)
					.build()
			)
		);
	}

	static Stream<Arguments> sortTypeParameters() {
		return Stream.of(
			// 성공 케이스
			Arguments.of("REVIEW", 200),
			Arguments.of("POPULAR", 200),
			Arguments.of("PICK", 200),
			Arguments.of("REVIEW", 200),
			// 실패 케이스
			Arguments.of("RATINGS", 400),
			Arguments.of("POpu", 400),
			Arguments.of("PIC", 400),
			Arguments.of("REVIEWWW", 400)
		);
	}

	static Stream<Arguments> sortOrderParameters() {
		return Stream.of(
			// 성공 케이스
			Arguments.of("ASC", 200),
			Arguments.of("DESC", 200),
			// 실패 케이스
			Arguments.of("DESCCC", 400),
			Arguments.of("ASCC", 400)
		);
	}

	@DisplayName("술(위스키) 리스트를 조회할 수 있다.")
	@ParameterizedTest(name = "[{index}]{0}")
	@MethodSource("testCase1Provider")
	void test_case_1(String description, AlcoholSearchRequest searchRequest) throws Exception {

		// given
		PageResponse<AlcoholSearchResponse> response = getResponse();

		// when
		when(alcoholQueryService.searchAlcohols(any(), any())).thenReturn(response);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/alcohols/search")
				.param("keyword", searchRequest.keyword())
				.param("category", searchRequest.category())
				.param("regionId", searchRequest.regionId() == null ? null : String.valueOf(searchRequest.regionId()))
				.param("sortType", searchRequest.sortType().name())
				.param("sortOrder", searchRequest.sortOrder().name())
				.param("cursor", String.valueOf(searchRequest.cursor()))
				.param("pageSize", String.valueOf(searchRequest.pageSize()))
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.totalCount").value(5));
		resultActions.andExpect(jsonPath("$.data.alcohols.size()").value(3));
		resultActions.andExpect(jsonPath("$.data.alcohols[0].alcoholId").value(5));
		resultActions.andExpect(jsonPath("$.data.alcohols[0].korName").value("아녹 24년"));
		resultActions.andExpect(jsonPath("$.data.alcohols[0].engName").value("anCnoc 24-year-old"));

	}

	@DisplayName("정렬 타입에 대한 검증")
	@ParameterizedTest(name = "{1} : {0}")
	@MethodSource("sortTypeParameters")
	void test_sortType(String sortType, int expectedStatus) throws Exception {
		// given
		PageResponse<AlcoholSearchResponse> response = getResponse();

		// when
		when(alcoholQueryService.searchAlcohols(any(), any())).thenReturn(response);

		mockMvc.perform(get("/api/v1/alcohols/search")
				.param("keyword", "")
				.param("category", "")
				.param("regionId", "")
				.param("sortType", sortType)
				.param("sortOrder", "DESC")
				.param("cursor", "")
				.param("pageSize", "")
				.with(csrf())
			)
			.andExpect(status().is(expectedStatus))
			.andDo(print());
	}

	@DisplayName("정렬 방향에 대한 검증")
	@ParameterizedTest(name = "{1} : {0}")
	@MethodSource("sortOrderParameters")
	void test_sortOrder(String sortOrder, int expectedStatus) throws Exception {
		// given
		PageResponse<AlcoholSearchResponse> response = getResponse();

		// when
		when(alcoholQueryService.searchAlcohols(any(), any())).thenReturn(response);

		mockMvc.perform(get("/api/v1/alcohols/search")
				.param("keyword", "")
				.param("category", "")
				.param("regionId", "")
				.param("sortType", "REVIEW")
				.param("sortOrder", sortOrder)
				.param("cursor", "")
				.param("pageSize", "")
				.with(csrf())
			)
			.andExpect(status().is(expectedStatus))
			.andDo(print());
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
