package app.bottlenote.docs.alcohols;

import app.bottlenote.alcohols.controller.RegionController;
import app.bottlenote.alcohols.dto.response.RegionsResponse;
import app.bottlenote.alcohols.service.AlcoholReferenceService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("region 컨트롤러 RestDocs용 테스트")
class RestRegionControllerTest extends AbstractRestDocs {

	private final AlcoholReferenceService regionService = mock(AlcoholReferenceService.class);

	@Override
	protected Object initController() {
		return new RegionController(regionService);
	}

	@Test
	@DisplayName("지역 리스트를 조회할 수 있다.")
	void findAllTest() throws Exception {
		// given
		List<RegionsResponse> response = List.of(
			RegionsResponse.of(1L, "스코틀랜드/로우랜드", "Scotland/Lowlands", "가벼운 맛이 특징인 로우랜드 위스키"),
			RegionsResponse.of(2L, "스코틀랜드/하이랜드", "Scotland/Highlands", "맛의 다양성이 특징인 하이랜드 위스키, 해안의 짠맛부터 달콤하고 과일 맛까지"),
			RegionsResponse.of(3L, "스코틀랜드/아일랜드", "Scotland/Ireland", "부드러운 맛이 특징인 아일랜드 위스키"),
			RegionsResponse.of(11L, "프랑스", "France", "주로 브랜디와 와인 생산지로 유명하지만 위스키도 생산"),
			RegionsResponse.of(12L, "스웨덴", "Sweden", "실험적인 방법으로 만드는 스웨덴 위스키")
		);
		// when
		when(regionService.findAllRegion()).thenReturn(response);

		// then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/regions/"))
			.andExpect(status().isOk())
			.andDo(
				document("alcohols/regions",
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data[].regionId").description("지역 ID"),
						fieldWithPath("data[].korName").description("지역 한글명"),
						fieldWithPath("data[].engName").description("지역 이름"),
						fieldWithPath("data[].description").description("지역 설명"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				));
	}
}
