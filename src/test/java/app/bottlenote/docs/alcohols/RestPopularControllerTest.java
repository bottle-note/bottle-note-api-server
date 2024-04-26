package app.bottlenote.docs.alcohols;

import app.bottlenote.alcohols.controller.PopularController;
import app.bottlenote.alcohols.dto.response.Populars;
import app.bottlenote.alcohols.service.PopularService;
import app.bottlenote.docs.AbstractRestDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;
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

@DisplayName("popular 컨트롤러 RestDocs용 테스트")
class RestPopularControllerTest extends AbstractRestDocs {

	private final PopularService popularService = mock(PopularService.class);


	@Override
	protected Object initController() {
		return new PopularController(popularService);
	}

	@DisplayName("주간 인기 술 리스트를 조회할 수 있다.")
	@Test
	void getWeeklyPopularAlcoholsTest() throws Exception {
		// given
		List<Populars> populars = List.of(
			Populars.of(1L, "글렌피딕", "glen fi", 3.5, "single molt", "https://i.imgur.com/TE2nmYV.png"),
			Populars.of(2L, "맥키토시", "macintosh", 4.5, "single molt", "https://i.imgur.com/TE2nmYV.png"),
			Populars.of(3L, "글렌리벳", "glen rivet", 4.0, "single molt", "https://i.imgur.com/TE2nmYV.png"),
			Populars.of(4L, "글렌피딕", "glen fi", 3.5, "single molt", "https://i.imgur.com/TE2nmYV.png"),
			Populars.of(5L, "맥키토시", "macintosh", 4.5, "single molt", "https://i.imgur.com/TE2nmYV.png")
		);
		// when & then
		when(popularService.getPopularOfWeek(5)).thenReturn(populars);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/popular/week/")
				.param("top", "5"))
			.andExpect(status().isOk())
			.andDo(
				document("alcohols/populars/week",
					queryParameters(
						parameterWithName("top").description("조회할 주간 인기 술 목록 사이즈(갯수)")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드(http status code)"),
						fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("주간 인기 술 리스트의 크기"),
						fieldWithPath("data.alcohols[].whiskyId").type(JsonFieldType.NUMBER).description("술 ID"),
						fieldWithPath("data.alcohols[].korName").type(JsonFieldType.STRING).description("술 이름"),
						fieldWithPath("data.alcohols[].engName").type(JsonFieldType.STRING).description("술 영문 이름"),
						fieldWithPath("data.alcohols[].rating").type(JsonFieldType.NUMBER).description("술 평점"),
						fieldWithPath("data.alcohols[].category").type(JsonFieldType.STRING).description("술 타입"),
						fieldWithPath("data.alcohols[].imageUrl").type(JsonFieldType.STRING).description("술 이미지 URL"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
						fieldWithPath("meta.serverEncoding").description("서버 인코딩 정도"),
						fieldWithPath("meta.serverVersion").description("서버 버전"),
						fieldWithPath("meta.serverPathVersion").description("서버 경로 버전"),
						fieldWithPath("meta.serverResponseTime").description("서버 응답 시간")
					)
				));
	}
}