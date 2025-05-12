package app.docs.alcohols;

import app.bottlenote.alcohols.controller.AlcoholPopularQueryController;
import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.alcohols.service.PopularService;
import app.docs.AbstractRestDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static app.bottlenote.alcohols.fixture.PopularsObjectFixture.getFixturePopulars;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("popular 컨트롤러 RestDocs용 테스트")
class RestPopularControllerIntegrationTest extends AbstractRestDocs {

	private final PopularService popularService = mock(PopularService.class);


	@Override
	protected Object initController() {
		return new AlcoholPopularQueryController(popularService);
	}

	@Test
	@DisplayName("주간 인기 술 리스트를 조회할 수 있다.")
	void docs_1() throws Exception {
		// given
		List<PopularItem> populars = List.of(
				getFixturePopulars(1L, "글렌피딕", "glen fi"),
				getFixturePopulars(2L, "맥키토시", "macintosh"),
				getFixturePopulars(3L, "글렌리벳", "glen rivet"),
				getFixturePopulars(4L, "글렌피딕", "glen fi"),
				getFixturePopulars(5L, "맥키토시", "macintosh")
		);


		// when & then
		when(popularService.getPopularOfWeek(anyInt(), any())).thenReturn(populars);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/popular/week/")
						.param("top", "5"))
				.andExpect(status().isOk())
				.andDo(
						document("alcohols/populars/week",
								queryParameters(
										parameterWithName("top").description("조회할 주간 인기 술 목록 사이즈(갯수)")
								),
								responseFields(
										fieldWithPath("success").description("응답 성공 여부"),
										fieldWithPath("code").description("응답 코드(http status code)"),
										fieldWithPath("data.totalCount").description("주간 인기 술 리스트의 크기"),
										fieldWithPath("data.alcohols").description("주간 인기 술 리스트"),
										fieldWithPath("data.alcohols[].alcoholId").description("술 ID"),
										fieldWithPath("data.alcohols[].korName").description("술 이름"),
										fieldWithPath("data.alcohols[].engName").description("술 영문명"),
										fieldWithPath("data.alcohols[].rating").description("술의 평균 평점"),
										fieldWithPath("data.alcohols[].ratingCount").description("술의 평점 참여자 수"),
										fieldWithPath("data.alcohols[].korCategory").description("술 카테고리"),
										fieldWithPath("data.alcohols[].engCategory").description("술 카테고리 영문명"),
										fieldWithPath("data.alcohols[].imageUrl").description("술 이미지 URL"),
										fieldWithPath("data.alcohols[].isPicked").description("내가 찜했는지 여부"),
										fieldWithPath("data.alcohols[].popularScore").description("인기도 점수"),

										fieldWithPath("errors").ignored(),
										fieldWithPath("meta.serverEncoding").ignored(),
										fieldWithPath("meta.serverVersion").ignored(),
										fieldWithPath("meta.serverPathVersion").ignored(),
										fieldWithPath("meta.serverResponseTime").ignored()
								)
						));

		verify(popularService).getPopularOfWeek(5, -1L);
	}

	@Test
	@DisplayName("봄에 인기 있는 술 리스트를 조회할 수 있다.")
	void docs_2() throws Exception {
		// given
		List<PopularItem> springPopulars = List.of(
				getFixturePopulars(1L, "글렌피딕", "glen fi"),
				getFixturePopulars(2L, "맥키토시", "macintosh"),
				getFixturePopulars(3L, "글렌리벳", "glen rivet"),
				getFixturePopulars(4L, "글렌피딕", "glen fi"),
				getFixturePopulars(5L, "맥키토시", "macintosh")
		);

		// when & then
		when(popularService.getSpringItems(anyLong())).thenReturn(springPopulars);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/popular/spring"))
				.andExpect(status().isOk())
				.andDo(
						document("alcohols/populars/spring",
								responseFields(
										fieldWithPath("success").description("응답 성공 여부"),
										fieldWithPath("code").description("응답 코드(http status code)"),

										fieldWithPath("data[].alcoholId").description("술 ID"),
										fieldWithPath("data[].korName").description("술 이름"),
										fieldWithPath("data[].engName").description("술 영문명"),
										fieldWithPath("data[].rating").description("술의 평균 평점"),
										fieldWithPath("data[].ratingCount").description("술의 평점 참여자 수"),
										fieldWithPath("data[].korCategory").description("술 카테고리"),
										fieldWithPath("data[].engCategory").description("술 카테고리 영문명"),
										fieldWithPath("data[].imageUrl").description("술 이미지 URL"),
										fieldWithPath("data[].isPicked").description("내가 찜했는지 여부"),
										fieldWithPath("data[].popularScore").description("인기도 점수"),

										fieldWithPath("errors").ignored(),
										fieldWithPath("meta.serverEncoding").ignored(),
										fieldWithPath("meta.serverVersion").ignored(),
										fieldWithPath("meta.serverPathVersion").ignored(),
										fieldWithPath("meta.serverResponseTime").ignored()
								)
						));
	}
}
