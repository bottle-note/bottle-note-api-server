package app.bottlenote.user.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.service.DefaultUserFacade;
import app.bottlenote.user.service.UserBasicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Tag("unit")
@DisplayName("[unit] [controller] UserProfileImagesChangeController")
@WebMvcTest(UserBasicController.class)
@ActiveProfiles("test")
@WithMockUser
class UserProfileImagesChangeControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;
  @MockBean private UserBasicService profileImageChangeService;
  @MockBean private DefaultUserFacade userFacade;

  private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

  @BeforeEach
  void setup() {
    mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
    mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtil.close();
  }

  @DisplayName("프로필 이미지를 성공적으로 변경할 수 있다.")
  @Test
  void test_1() throws Exception {

    Long userId = 1L;
    String viewUrl = "http://example.com/profile-image.jpg";

    ProfileImageChangeResponse response = new ProfileImageChangeResponse(userId, viewUrl);

    when(profileImageChangeService.profileImageChange(anyLong(), anyString())).thenReturn(response);

    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("viewUrl", viewUrl);

    mockMvc
        .perform(
            patch("/api/v1/users/profile-image")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(requestBody))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true)) // 수정된 부분
        .andExpect(jsonPath("$.code").value(200)) // 수정된 부분
        .andExpect(jsonPath("$.data.userId").value(response.userId()))
        .andExpect(jsonPath("$.data.profileImageUrl").value(response.profileImageUrl()))
        .andDo(print());
  }
}
