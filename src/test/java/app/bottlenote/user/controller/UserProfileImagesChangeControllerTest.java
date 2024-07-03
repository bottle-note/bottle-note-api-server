package app.bottlenote.user.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.dto.request.ProfileImageChangeRequest;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.service.UserCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCommandController.class)
@WithMockUser
public class UserProfileImagesChangeControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper mapper;
	@MockBean
	private UserCommandService profileImageChangeService;

	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
		when(SecurityContextUtil.getCurrentUserId()).thenReturn(1L);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("프로필 이미지를 성공적으로 변경할 수 있다.")
	void test_1() throws Exception {

		Long userId = 1L;

		ProfileImageChangeRequest request = new ProfileImageChangeRequest("http://example.com/new-profile-image.jpg");
		ProfileImageChangeResponse response = ProfileImageChangeResponse.builder()
			.userId(userId)
			.profileImageUrl("http://example.com/new-profile-image.jpg")
			.callback("https://bottle-note.com/api/v1/users/" + userId)
			.build();

		when(profileImageChangeService.profileImageChange(userId, request)).thenReturn(response);

		mockMvc.perform(patch("/api/v1/users/profile-image")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value("true"))
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.userId").value(response.getUserId()))
			.andExpect(jsonPath("$.data.profileImageUrl").value(response.getProfileImageUrl()))
			.andExpect(jsonPath("$.data.callback").value(response.getCallback().toString()))
			.andDo(print());

	}

}
