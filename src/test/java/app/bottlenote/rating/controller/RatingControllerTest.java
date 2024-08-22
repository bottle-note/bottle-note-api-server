package app.bottlenote.rating.controller;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.service.RatingCommandService;
import app.bottlenote.rating.service.RatingQueryService;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserExceptionCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@Tag("unit")
@DisplayName("[unit] [controller] RatingController")
@WebMvcTest(RatingController.class)
class RatingControllerTest {
	private final Long userId = 1L;
	private final Long alcoholId = 1L;
	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private RatingQueryService queryService;
	@MockBean
	private RatingCommandService commandService;
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;
	private User user;
	private Alcohol alcohol;
	private Rating rating;


	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);

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
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value("true"))
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.rating").value(rating.getRatingPoint().getRating().toString()))
			.andExpect(jsonPath("$.data.message").value(RatingRegisterResponse.Message.SUCCESS.getMessage()));

	}

	@Test
	@DisplayName("등록 시 유저 정보가 없을 경우 예외를 발생시킨다.")
	void test_2() throws Exception {

		Error error = Error.of(UserExceptionCode.REQUIRED_USER_ID);

		// given
		RatingRegisterRequest request = new RatingRegisterRequest(alcoholId, 5.0);

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.empty());

		// then
		mockMvc.perform(post("/api/v1/rating/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andDo(print())
			.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
			.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
			.andExpect(jsonPath("$.errors[0].message").value(error.message()));
	}

	@Test
	@DisplayName("별점 등록 시 파라미터가 없는 경우 예외를 발생시킨다.")
	void test_3() throws Exception {
		// Expected errors
		Error ratingError = Error.of(ValidExceptionCode.RATING_REQUIRED);
		Error alcoholIdError = Error.of(ValidExceptionCode.ALCOHOL_ID_REQUIRED);

		// given
		RatingRegisterRequest request = new RatingRegisterRequest(null, null);

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		// then
		mockMvc.perform(post("/api/v1/rating/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors", hasSize(2)))
			.andExpect(jsonPath("$.errors[?(@.code == 'RATING_REQUIRED')].status").value(ratingError.status().name()))
			.andExpect(jsonPath("$.errors[?(@.code == 'RATING_REQUIRED')].message").value(ratingError.message()))
			.andExpect(jsonPath("$.errors[?(@.code == 'ALCOHOL_ID_REQUIRED')].status").value(alcoholIdError.status().name()))
			.andExpect(jsonPath("$.errors[?(@.code == 'ALCOHOL_ID_REQUIRED')].message").value(alcoholIdError.message()));
	}


}
