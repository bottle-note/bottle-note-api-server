package app.bottlenote.support.help.controller;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.exception.HelpException;
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
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_AUTHORIZED;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_FOUND;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

	private final HelpUpsertRequest helpUpsertRequest = HelpObjectFixture.getHelpUpsertRequest();
	private final HelpPageableRequest emptyPageableRequest = HelpObjectFixture.getEmptyHelpPageableRequest();
	private final PageResponse<HelpListResponse> helpPageResponse = HelpObjectFixture.getHelpListPageResponse();
	private final HelpResultResponse successRegisterResponse = HelpObjectFixture.getSuccessHelpResponse(REGISTER_SUCCESS);
	private final HelpResultResponse successModifyResponse = HelpObjectFixture.getSuccessHelpResponse(MODIFY_SUCCESS);
	private final HelpResultResponse successDeleteResponse = HelpObjectFixture.getSuccessHelpResponse(DELETE_SUCCESS);

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
			when(helpService.registerHelp(helpUpsertRequest, 1L))
				.thenReturn(successRegisterResponse);

			// then
			mockMvc.perform(post("/api/v1/help")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(helpUpsertRequest))
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

			when(helpService.registerHelp(helpUpsertRequest, 1L))
				.thenReturn(successRegisterResponse);

			// then
			mockMvc.perform(post("/api/v1/help")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(helpUpsertRequest))
					.with(csrf()))
				.andExpect(status().isBadRequest())
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("문의글 조회 컨트롤러 테스트")
	class HelpReadControllerTest{

		@DisplayName("문의글 목록을 조회할 수 있다.")
		@Test
		void get_help_list_test() throws Exception {
		    // given
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		    // when
			when(helpService.getHelpList(any(HelpPageableRequest.class), anyLong()))
				.thenReturn(helpPageResponse);

		    // then
			ResultActions resultActions = mockMvc.perform(get("/api/v1/help")
					.param("cursor", String.valueOf(emptyPageableRequest.cursor()))
					.param("pageSize", String.valueOf(emptyPageableRequest.pageSize()))
					.with(csrf())
				)
				.andExpect(status().isOk())
				.andDo(print());

			resultActions.andExpect(jsonPath("$.success").value("true"));
			resultActions.andExpect(jsonPath("$.code").value("200"));
			resultActions.andExpect(jsonPath("$.data.totalCount").value(2));
		}

		@DisplayName("문의글을 상세 조회할 수 있다.")
		@Test
		void get_detail_help_test() throws Exception {
		    // given
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			// when
			when(helpService.getDetailHelp(anyLong(), anyLong()))
				.thenReturn(HelpObjectFixture.getDetailHelpInfo("title","content", HelpType.REVIEW));

			// then
			ResultActions resultActions = mockMvc.perform(get("/api/v1/help/{helpId}", 1L)
					.param("cursor", String.valueOf(emptyPageableRequest.cursor()))
					.param("pageSize", String.valueOf(emptyPageableRequest.pageSize()))
					.with(csrf())
				)
				.andExpect(status().isOk())
				.andDo(print());

			resultActions.andExpect(jsonPath("$.success").value("true"));
			resultActions.andExpect(jsonPath("$.code").value("200"));
			resultActions.andExpect(jsonPath("$.data.helpType").value("REVIEW"));
		}

		@DisplayName("로그인 한 유저만 문의글을 조회할 수 있다.")
		@Test
		void get_help_only_authorized_user() throws Exception {

			// when
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.empty());
			when(helpService.getDetailHelp(anyLong(), anyLong()))
				.thenReturn(HelpObjectFixture.getDetailHelpInfo("title","content", HelpType.REVIEW));

			// then
			mockMvc.perform(get("/api/v1/help/{helpId}", 1L)
					.param("cursor", String.valueOf(emptyPageableRequest.cursor()))
					.param("pageSize", String.valueOf(emptyPageableRequest.pageSize()))
					.with(csrf())
				)
				.andExpect(status().isBadRequest())
				.andDo(print());
		}

	}
	@Nested
	@DisplayName("문의글 수정 컨트롤러 테스트")
	class HelpUpdateControllerTest{

		@DisplayName("문의글을 수정할 수 있다.")
		@Test
		void update_help_test() throws Exception {
			// given
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			// when
			when(helpService.modifyHelp(helpUpsertRequest, 1L, 1L))
				.thenReturn(successModifyResponse);

			// then
			mockMvc.perform(patch("/api/v1/help/{helpId}", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(helpUpsertRequest))
					.with(csrf()))
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(jsonPath("$.code").value("200"))
				.andExpect(jsonPath("$.data.message").value(MODIFY_SUCCESS.getDescription()));
		}

		@DisplayName("자신이 등록한 리뷰만 수정할 수 있다.")
		@Test
		void fail_update_help_test() throws Exception {

			Error error = Error.of(HELP_NOT_FOUND);
			// given
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			// when
			when(helpService.modifyHelp(helpUpsertRequest, 1L, 1L))
				.thenThrow(new HelpException(HELP_NOT_FOUND));

			// then
			mockMvc.perform(patch("/api/v1/help/{helpId}", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(helpUpsertRequest))
					.with(csrf()))
				.andExpect(status().isBadRequest())
				.andDo(print())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
				.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[0].message").value(error.message()));
		}

		@DisplayName("로그인하지 않은 유저는 문의글을 수정할 수 없다.")
		@Test
		void test_fail_when_unauthorized_user() throws Exception {
			// given
			Error error = Error.of(REQUIRED_USER_ID);
			// when
			when(SecurityContextUtil.getUserIdByContext())
				.thenReturn(Optional.empty());

			when(helpService.registerHelp(helpUpsertRequest, 1L))
				.thenReturn(successModifyResponse);

			// then
			mockMvc.perform(patch("/api/v1/help/{helpId}",1L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(helpUpsertRequest))
					.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
				.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[0].message").value(error.message()));
		}
	}

	@Nested
	@DisplayName("문의글 삭제 컨트롤러 테스트")
	class HelpDeleteControllerTest {

		@DisplayName("문의글을 삭제할 수 있다.")
		@Test
		void delete_help_test() throws Exception {
			// given
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			// when
			when(helpService.deleteHelp(1L, 1L))
				.thenReturn(successDeleteResponse);

			// then
			mockMvc.perform(delete("/api/v1/help/{helpId}", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(jsonPath("$.code").value("200"))
				.andExpect(jsonPath("$.data.message").value(DELETE_SUCCESS.getDescription()));
		}

		@DisplayName("존재하지 않는 문의글을 삭제할 수 없다.")
		@Test
		void test_fail_when_help_is_not_exist() throws Exception {
			// given
			Error error = Error.of(HELP_NOT_FOUND);
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			// when
			when(helpService.deleteHelp(1L, 1L))
				.thenThrow(new HelpException(HELP_NOT_FOUND));

			// then
			mockMvc.perform(delete("/api/v1/help/{helpId}", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andExpect(status().isBadRequest())
				.andDo(print())
				.andExpect(jsonPath("$.code").value("400"))
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
				.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[0].message").value(error.message()));
		}

		@DisplayName("자신이 등록한 리뷰만 삭제할 수 있다.")
		@Test
		void test_fail_when_user_is_not_owner() throws Exception {
			// given
			Error error = Error.of(HELP_NOT_AUTHORIZED);
			when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

			// when
			when(helpService.deleteHelp(1L, 1L))
				.thenThrow(new HelpException(HELP_NOT_AUTHORIZED));

			// then
			mockMvc.perform(delete("/api/v1/help/{helpId}", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andExpect(status().isUnauthorized())
				.andDo(print())
				.andExpect(jsonPath("$.code").value("401"))
				.andExpect(jsonPath("$.success").value("false"))
				.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
				.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[0].message").value(error.message()));
		}
	}

	@DisplayName("로그인하지 않은 유저는 문의글을 삭제할 수 없다.")
	@Test
	void test_fail_when_unauthorized_user() throws Exception {
		// given
		Error error = Error.of(REQUIRED_USER_ID);
		// when
		when(SecurityContextUtil.getUserIdByContext())
			.thenReturn(Optional.empty());

		when(helpService.deleteHelp(1L, 1L))
			.thenReturn(successDeleteResponse);

		// then
		mockMvc.perform(delete("/api/v1/help/{helpId}",1L)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
			.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
			.andExpect(jsonPath("$.errors[0].message").value(error.message()));
	}
}
