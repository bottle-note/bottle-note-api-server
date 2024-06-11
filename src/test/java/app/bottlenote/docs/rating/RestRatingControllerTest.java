package app.bottlenote.docs.rating;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.rating.controller.RatingController;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.fixture.RatingObjectFixture;
import app.bottlenote.rating.service.RatingCommandService;
import app.bottlenote.rating.service.RatingQueryService;
import app.bottlenote.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("별점 RestDocs용 테스트")
public class RestRatingControllerTest extends AbstractRestDocs {

	private final RatingQueryService queryService = mock(RatingQueryService.class);
	private final RatingCommandService commandService = mock(RatingCommandService.class);
	private final Long userId = 1L;
	private final Long alcoholId = 1L;
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	private User user;
	private Alcohol alcohol;
	private Rating rating;

	@Override
	protected Object initController() {
		return new RatingController(commandService, queryService);
	}

	@BeforeEach
	void setup() {
		user = User.builder().id(userId).build();
		alcohol = Alcohol.builder().id(alcoholId).build();
		rating = Rating.builder().id(RatingId.is(userId, alcoholId)).ratingPoint(RatingPoint.of(5)).build();
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Test
	@DisplayName("별점을 등록할 수 있다.")
	void test_1() throws Exception {
		// given
		RatingRegisterRequest request = new RatingRegisterRequest(alcoholId, 5.0);
		RatingRegisterResponse response = RatingRegisterResponse.success(rating);

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
		when(commandService.register(anyLong(), anyLong(), any(RatingPoint.class))).thenReturn(response);

		// then
		mockMvc.perform(post("/api/v1/rating/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("rating/register",
				requestFields(
					fieldWithPath("alcoholId").description("별점을 줄 위스키의 식별자"),
					fieldWithPath("rating").description("별점 ( 0.0 ~ 5.0 사이의 값)")
				),
				responseFields(
					fieldWithPath("success").description("응답 성공 여부"),
					fieldWithPath("code").description("응답 코드(http status code)"),
					fieldWithPath("data.rating").description("등록된 별점"),
					fieldWithPath("data.message").description("결과 메시지"),
					fieldWithPath("errors").ignored(),
					fieldWithPath("meta.serverEncoding").ignored(),
					fieldWithPath("meta.serverVersion").ignored(),
					fieldWithPath("meta.serverPathVersion").ignored(),
					fieldWithPath("meta.serverResponseTime").ignored()
				)
			));
	}

	@Test
	@DisplayName("별점 목록을 조회할 수 있다.")
	void test_2() throws Exception {
		// given
		RatingListFetchRequest request = RatingObjectFixture.ratingListFetchRequest("위스키", "위스키", 1L);
		RatingListFetchResponse fetchList = RatingObjectFixture.ratingListFetchResponse();
		CursorPageable pageable = CursorPageable.builder().currentCursor(0L).cursor(0L).pageSize(10L).hasNext(false).build();
		PageResponse<RatingListFetchResponse> response = PageResponse.of(fetchList, pageable);

		// when
		when(queryService.fetchRatingList(any(), any())).thenReturn(response);

		// then
		mockMvc.perform(get("/api/v1/rating")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(document("rating/fetch",
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
					fieldWithPath("data.ratings[].alcoholId").description("술 ID"),
					fieldWithPath("data.ratings[].imageUrl").description("술 이미지 URL"),
					fieldWithPath("data.ratings[].korName").description("술 한글 이름"),
					fieldWithPath("data.ratings[].engName").description("술 영문 이름"),
					fieldWithPath("data.ratings[].korCategoryName").description("술 한글 카테고리 이름"),
					fieldWithPath("data.ratings[].engCategoryName").description("술 영문 카테고리 이름"),
					fieldWithPath("data.ratings[].isPicked").description("술 찜 여부"),
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
					fieldWithPath("meta.searchParameters.cursor").description("검색 시 사용 한 커서 기준 "),
					fieldWithPath("meta.searchParameters.pageSize").description("검색 시 사용 한 페이지 사이즈")
				)
			));
	}
}
