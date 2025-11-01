package app.bottlenote.alcohols.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.alcohols.dto.request.CurationKeywordSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordDto;
import app.bottlenote.alcohols.service.AlcoholReferenceService;
import app.external.docs.AbstractRestDocs;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Tag("rest-docs")
@DisplayName("큐레이션 키워드 API 문서화 테스트")
class AlcoholReferenceControllerRestDocsTest extends AbstractRestDocs {

	private final AlcoholReferenceService alcoholReferenceService = mock(AlcoholReferenceService.class);

	@Override
	protected Object initController() {
		return new AlcoholReferenceController(alcoholReferenceService);
	}

	@Test
	@DisplayName("큐레이션 키워드 목록을 조회할 수 있다")
	void searchCurationKeywords() throws Exception {
		// given
		CurationKeywordDto dto1 = CurationKeywordDto.builder()
			.id(1L)
			.name("봄 추천 위스키")
			.description("봄에 어울리는 위스키 모음")
			.alcoholCount(10)
			.displayOrder(1)
			.build();

		CurationKeywordDto dto2 = CurationKeywordDto.builder()
			.id(2L)
			.name("여름 추천 위스키")
			.description("여름에 어울리는 위스키 모음")
			.alcoholCount(8)
			.displayOrder(2)
			.build();

		CursorPageable pageable = CursorPageable.builder()
			.currentCursor(0L)
			.cursor(10L)
			.pageSize(10L)
			.hasNext(false)
			.build();

		CursorResponse<CurationKeywordDto> response =
			CursorResponse.of(List.of(dto1, dto2), pageable);

		when(alcoholReferenceService.searchCurationKeywords(any(CurationKeywordSearchRequest.class)))
			.thenReturn(response);

		// when
		ResultActions resultActions = mockMvc.perform(
			MockMvcRequestBuilders.get("/api/v1/curations")
				.param("keyword", "봄")
				.param("cursor", "0")
				.param("pageSize", "10")
		);

		// then
		resultActions.andExpect(status().isOk())
			.andDo(document("curation-keywords-search",
				queryParameters(
					parameterWithName("keyword").description("큐레이션 키워드 이름 검색 (부분 일치)").optional(),
					parameterWithName("alcoholId").description("위스키 식별자로 검색 - 해당 ID의 위스키가 포함된 큐레이션 조회 (예: alcoholId=123 검색 시 ID가 123인 위스키가 포함된 모든 큐레이션 반환)").optional(),
					parameterWithName("cursor").description("커서 페이징 (기본값: 0)").optional(),
					parameterWithName("pageSize").description("페이지 크기 (기본값: 10)").optional()
				),
				responseFields(
					fieldWithPath("code").type(NUMBER).description("응답 코드"),
					fieldWithPath("data").type(OBJECT).description("응답 데이터"),
					fieldWithPath("data.items").type(ARRAY).description("큐레이션 키워드 목록"),
					fieldWithPath("data.items[].id").type(NUMBER).description("큐레이션 ID"),
					fieldWithPath("data.items[].name").type(STRING).description("큐레이션 이름"),
					fieldWithPath("data.items[].description").type(STRING).description("큐레이션 설명"),
					fieldWithPath("data.items[].alcoholCount").type(NUMBER).description("포함된 위스키 개수"),
					fieldWithPath("data.items[].displayOrder").type(NUMBER).description("노출 순서"),
					fieldWithPath("data.pageable").type(OBJECT).description("페이징 정보"),
					fieldWithPath("data.pageable.currentCursor").type(NUMBER).description("현재 커서"),
					fieldWithPath("data.pageable.cursor").type(NUMBER).description("다음 커서"),
					fieldWithPath("data.pageable.pageSize").type(NUMBER).description("페이지 크기"),
					fieldWithPath("data.pageable.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
					fieldWithPath("errors").description("에러 정보").optional()
				)
			));
	}

	@Test
	@DisplayName("특정 큐레이션의 위스키 목록을 조회할 수 있다")
	void getCurationAlcohols() throws Exception {
		// given
		AlcoholsSearchItem item = AlcoholsSearchItem.builder()
			.alcoholId(1L)
			.korName("글렌피딕 15년")
			.engName("Glenfiddich 15")
			.korCategoryName("싱글 몰트")
			.engCategoryName("Single Malt")
			.imageUrl("https://example.com/image.jpg")
			.rating(4.5)
			.ratingCount(100L)
			.reviewCount(50L)
			.pickCount(30L)
			.isPicked(false)
			.build();

		CursorPageable pageable = CursorPageable.builder()
			.currentCursor(0L)
			.cursor(10L)
			.pageSize(10L)
			.hasNext(false)
			.build();

		CursorResponse<AlcoholsSearchItem> response =
			CursorResponse.of(List.of(item), pageable);

		when(alcoholReferenceService.getCurationAlcohols(any(), any(), any()))
			.thenReturn(response);

		// when
		ResultActions resultActions = mockMvc.perform(
			MockMvcRequestBuilders.get("/api/v1/curations/{curationId}/alcohols", 1L)
				.param("cursor", "0")
				.param("pageSize", "10")
		);

		// then
		resultActions.andExpect(status().isOk())
			.andDo(document("curation-alcohols-get",
				pathParameters(
					parameterWithName("curationId").description("큐레이션 ID")
				),
				queryParameters(
					parameterWithName("cursor").description("커서 페이징 (기본값: 0)").optional(),
					parameterWithName("pageSize").description("페이지 크기 (기본값: 10)").optional()
				),
				responseFields(
					fieldWithPath("code").type(NUMBER).description("응답 코드"),
					fieldWithPath("data").type(OBJECT).description("응답 데이터"),
					fieldWithPath("data.items").type(ARRAY).description("위스키 목록"),
					fieldWithPath("data.items[].alcoholId").type(NUMBER).description("위스키 ID"),
					fieldWithPath("data.items[].korName").type(STRING).description("위스키 한글명"),
					fieldWithPath("data.items[].engName").type(STRING).description("위스키 영문명"),
					fieldWithPath("data.items[].korCategoryName").type(STRING).description("카테고리 한글명"),
					fieldWithPath("data.items[].engCategoryName").type(STRING).description("카테고리 영문명"),
					fieldWithPath("data.items[].imageUrl").type(STRING).description("이미지 URL"),
					fieldWithPath("data.items[].rating").type(NUMBER).description("평점"),
					fieldWithPath("data.items[].ratingCount").type(NUMBER).description("평점 개수"),
					fieldWithPath("data.items[].reviewCount").type(NUMBER).description("리뷰 개수"),
					fieldWithPath("data.items[].pickCount").type(NUMBER).description("찜 개수"),
					fieldWithPath("data.items[].isPicked").type(BOOLEAN).description("찜 여부"),
					fieldWithPath("data.pageable").type(OBJECT).description("페이징 정보"),
					fieldWithPath("data.pageable.currentCursor").type(NUMBER).description("현재 커서"),
					fieldWithPath("data.pageable.cursor").type(NUMBER).description("다음 커서"),
					fieldWithPath("data.pageable.pageSize").type(NUMBER).description("페이지 크기"),
					fieldWithPath("data.pageable.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
					fieldWithPath("errors").description("에러 정보").optional()
				)
			));
	}
}
