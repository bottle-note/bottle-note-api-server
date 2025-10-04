package app.bottlenote.follow.controller;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.controller.FollowController;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowUpdateResponse;
import app.bottlenote.user.exception.FollowException;
import app.bottlenote.user.exception.FollowExceptionCode;
import app.bottlenote.user.service.FollowService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.ResultActions;

@Tag("unit")
@DisplayName("[unit] [controller] FollowUpdateController")
@WebMvcTest(FollowController.class)
@ActiveProfiles("test")
@WithMockUser
class FollowUpdateControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;
  @MockBean private FollowService followService;

  private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

  @BeforeEach
  void setup() {
    mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
    mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(9L));
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtil.close();
  }

  @DisplayName("다른 유저를 팔로우 할 수 있다.")
  @Test
  void test_1() throws Exception {

    // given
    FollowUpdateRequest request = new FollowUpdateRequest(1L, FollowStatus.FOLLOWING);
    FollowUpdateResponse response =
        FollowUpdateResponse.builder()
            .status(FollowStatus.FOLLOWING)
            .followUserId(1L)
            .nickName("nickName")
            .imageUrl("imageUrl")
            .build();

    // when
    when(followService.updateFollowStatus(request, 9L)).thenReturn(response);

    // then
    ResultActions resultActions =
        mockMvc
            .perform(
                post("/api/v1/follow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
                    .with(csrf()))
            .andExpect(status().isOk())
            .andDo(print());

    resultActions.andExpect(jsonPath("$.success").value("true"));
    resultActions.andExpect(jsonPath("$.code").value("200"));
    resultActions.andExpect(jsonPath("$.data.followUserId").value(response.getFollowUserId()));
    resultActions.andExpect(jsonPath("$.data.nickName").value(response.getNickName()));
    resultActions.andExpect(jsonPath("$.data.imageUrl").value(response.getImageUrl()));
    resultActions.andExpect(jsonPath("$.data.message").value(response.getMessage()));
  }

  @DisplayName("유저를 언팔로우할 수 있다.")
  @Test
  void test_2() throws Exception {
    // given
    FollowUpdateRequest request = new FollowUpdateRequest(1L, FollowStatus.UNFOLLOW);
    FollowUpdateResponse response =
        FollowUpdateResponse.builder()
            .status(FollowStatus.UNFOLLOW)
            .followUserId(1L)
            .nickName("nickName")
            .imageUrl("imageUrl")
            .build();

    // when
    when(followService.updateFollowStatus(request, 9L)).thenReturn(response);

    // then
    ResultActions resultActions =
        mockMvc
            .perform(
                post("/api/v1/follow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
                    .with(csrf()))
            .andExpect(status().isOk())
            .andDo(print());

    resultActions.andExpect(jsonPath("$.success").value("true"));
    resultActions.andExpect(jsonPath("$.code").value("200"));
    resultActions.andExpect(jsonPath("$.data.followUserId").value(response.getFollowUserId()));
    resultActions.andExpect(jsonPath("$.data.nickName").value(response.getNickName()));
    resultActions.andExpect(jsonPath("$.data.imageUrl").value(response.getImageUrl()));
    resultActions.andExpect(jsonPath("$.data.message").value(response.getMessage()));
  }

  @DisplayName("자기 자신을 팔로우할 수 없다.")
  @Test
  void test_3() throws Exception {

    Error error = Error.of(FollowExceptionCode.CANNOT_FOLLOW_SELF);

    // given
    FollowUpdateRequest request = new FollowUpdateRequest(9L, FollowStatus.FOLLOWING);

    // when
    when(followService.updateFollowStatus(request, 9L))
        .thenThrow(new FollowException(FollowExceptionCode.CANNOT_FOLLOW_SELF));

    // then
    ResultActions resultActions =
        mockMvc
            .perform(
                post("/api/v1/follow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andDo(print());

    resultActions.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())));
    resultActions.andExpect(jsonPath("$.errors[0].status").value(error.status().name()));
    resultActions.andExpect(jsonPath("$.errors[0].message").value(error.message()));
  }

  @DisplayName("팔로우할 유저가 존재하지 않으면 팔로우할 수 없다.")
  @Test
  void test_4() throws Exception {
    Error error = Error.of(FollowExceptionCode.FOLLOW_NOT_FOUND);
    // given
    FollowUpdateRequest request = new FollowUpdateRequest(1L, FollowStatus.FOLLOWING);

    // when
    when(followService.updateFollowStatus(request, 9L))
        .thenThrow(new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

    // then
    ResultActions resultActions =
        mockMvc
            .perform(
                post("/api/v1/follow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andDo(print());

    resultActions.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())));
    resultActions.andExpect(jsonPath("$.errors[0].status").value(error.status().name()));
    resultActions.andExpect(jsonPath("$.errors[0].message").value(error.message()));
  }
}
