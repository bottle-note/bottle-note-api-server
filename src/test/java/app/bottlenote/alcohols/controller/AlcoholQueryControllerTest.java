package app.bottlenote.alcohols.controller;


import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.fixture.AlcoholQueryFixture;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.service.cursor.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@WithMockUser()
@DisplayName("{unit}{Controller} AlcoholQuery")
@WebMvcTest(AlcoholQueryController.class)
class AlcoholQueryControllerTest {
	private static final Logger log = LogManager.getLogger(AlcoholQueryControllerTest.class);
	private final AlcoholQueryFixture fixture = new AlcoholQueryFixture();
	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private AlcoholQueryService alcoholQueryService;


	@DisplayName("술(위스키) 리스트를 조회할 수 있다.")
	@ParameterizedTest(name = "[{index}]{0}")
	@MethodSource("app.bottlenote.alcohols.fixture.ArgumentsFixture#testCase1Provider")
	void test_case_1(String description, AlcoholSearchRequest searchRequest) throws Exception {
		log.debug("description test : {}", description);
		// given
		PageResponse<AlcoholSearchResponse> response = fixture.getResponse();

		// when
		when(alcoholQueryService.searchAlcohols(any(), any())).thenReturn(response);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/alcohols/search")
				.param("keyword", searchRequest.keyword())
				.param("category", searchRequest.category() == null ? null : searchRequest.category().name())
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
	@MethodSource("app.bottlenote.alcohols.fixture.ArgumentsFixture#sortTypeParameters")
	void test_sortType(String sortType, int expectedStatus) throws Exception {
		// given
		PageResponse<AlcoholSearchResponse> response = fixture.getResponse();

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
	@MethodSource("app.bottlenote.alcohols.fixture.ArgumentsFixture#sortOrderParameters")
	void test_sortOrder(String sortOrder, int expectedStatus) throws Exception {
		// given
		PageResponse<AlcoholSearchResponse> response = fixture.getResponse();

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

}
