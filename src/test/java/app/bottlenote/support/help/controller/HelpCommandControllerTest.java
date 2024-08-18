package app.bottlenote.support.help.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.help.dto.request.HelpRegisterRequest;
import app.bottlenote.support.help.dto.response.HelpRegisterResponse;
import app.bottlenote.support.help.fixture.HelpObjectFixture;
import app.bottlenote.support.help.service.HelpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@DisplayName("[unit] [controller] HelpCommandController")
@WebMvcTest(HelpCommandController.class)
@WithMockUser
class HelpCommandControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private HelpService helpService;
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	private HelpRegisterRequest helpRegisterRequest = HelpObjectFixture.getHelpRegisterRequest();
	private HelpRegisterResponse successResponse = HelpObjectFixture.getSuccessHelpRegisterResponse();

	private final Long userId = 1L;

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	}


	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Nested
	@DisplayName("문의글 등록 컨트롤러 테스트")
	class HelpRegisterControllerTest {

		@DisplayName("문의글을 등록할 수 있다.")
		@Test
		void register_help_test() throws Exception {

			// given
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			// when
			when(helpService.registerHelp(helpRegisterRequest, 1L))
				.thenReturn(successResponse);

			// then
			mockMvc.perform(post("/api/v1/help")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(helpRegisterRequest))
					.with(csrf()))
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(jsonPath("$.code").value("200"))
				.andExpect(jsonPath("$.data.message").value(REGISTER_SUCCESS.getDescription()));
		}

		@DisplayName("로그인하지 않은 유저는 문의글을 등록할 수 없다.")
		@Test
		void test_fail_when_unauthorized_user() throws Exception {
			// given
			// when
			when(SecurityContextUtil.getUserIdByContext())
				.thenReturn(Optional.empty());

			when(helpService.registerHelp(helpRegisterRequest, 1L))
				.thenReturn(successResponse);

			// then
			mockMvc.perform(post("/api/v1/help")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(helpRegisterRequest))
					.with(csrf()))
				.andExpect(status().isBadRequest())
				.andDo(print());
		}

		@DisplayName("문의글의 제목은 null일 수 없다.")
		@Test
		void test_fail_when_invalidate_request() throws Exception {

			// when
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			HelpRegisterRequest wrongTitleRegisterRequest = HelpObjectFixture.getWrongTitleRegisterRequest();

			when(helpService.registerHelp(wrongTitleRegisterRequest, 1L))
				.thenReturn(successResponse);

			// then
			mockMvc.perform(post("/api/v1/help")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(wrongTitleRegisterRequest))
					.with(csrf()))
				.andExpect(status().isBadRequest())
				.andDo(print())
				.andExpect(status().isBadRequest());
//				.andExpect(jsonPath("$.code").value("400"))
//				.andExpect(jsonPath("$.success").value("false"));
		}
	}
}
