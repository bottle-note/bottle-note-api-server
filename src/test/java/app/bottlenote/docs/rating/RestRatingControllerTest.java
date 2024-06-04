package app.bottlenote.docs.rating;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.rating.controller.RatingController;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
}
