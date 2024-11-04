package app.external.docs.push.ui;

import app.bottlenote.global.security.SecurityContextUtil;
import app.external.docs.AbstractRestDocs;
import app.external.push.domain.Platform;
import app.external.push.dto.model.TokenMessage;
import app.external.push.dto.request.TokenSaveRequest;
import app.external.push.dto.response.TokenSaveResponse;
import app.external.push.service.PushHandler;
import app.external.push.service.UserDeviceService;
import app.external.push.ui.NotificationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerRestDocsTest extends AbstractRestDocs {
	private final PushHandler pushHandler = mock(PushHandler.class);
	private final UserDeviceService deviceService = mock(UserDeviceService.class);
	private final MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);

	@Override
	protected Object initController() {
		return new NotificationController(pushHandler, deviceService);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Nested
	@DisplayName("사용자 디바이스 토큰을 관리할 수 있다.")
	class Describe_saveUserToken {

		@Test
		@DisplayName("사용자 디바이스 토큰을 저장할 수 있다.")
		void test_1() throws Exception {
			// given
			final Long userId = 1L;
			final String token = UUID.randomUUID().toString();
			final Platform platform = Platform.IOS;
			TokenSaveRequest request = new TokenSaveRequest(token, platform);
			mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext)
				.thenReturn(Optional.of(userId));
			when(deviceService.saveUserToken(userId, request.deviceToken(), request.platform()))
				.thenReturn(TokenSaveResponse.of(token, platform, TokenMessage.DEVICE_TOKEN_SAVED));

			// when
			ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/push/token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
					.with(csrf()))
				.andExpect(status().isOk())
				.andDo(
					document("push/save-user-token",
						requestFields(
							fieldWithPath("deviceToken").description("저장될 디바이스 토큰"),
							fieldWithPath("platform").description("디바이스 정보 (IOS, ANDROID)")
						),
						responseFields(
							fieldWithPath("success").type(BOOLEAN).description("요청 성공 여부"),
							fieldWithPath("code").type(NUMBER).description("응답 코드"),
							fieldWithPath("data").type(OBJECT).description("응답 데이터"),
							fieldWithPath("data.deviceToken").type(STRING).description("저장된 디바이스 토큰"),
							fieldWithPath("data.platform").type(STRING).description("디바이스 정보 (IOS, ANDROID)"),
							fieldWithPath("data.message").type(STRING).description("응답 메시지"),
							fieldWithPath("errors").type(ARRAY).ignored(),
							fieldWithPath("meta").type(OBJECT).ignored(),
							fieldWithPath("meta.serverVersion").ignored(),
							fieldWithPath("meta.serverEncoding").ignored(),
							fieldWithPath("meta.serverResponseTime").ignored(),
							fieldWithPath("meta.serverPathVersion").ignored()
						)
					)
				);

			// then
			Assertions.assertNotNull(resultActions);
			Assertions.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
		}
	}
}
