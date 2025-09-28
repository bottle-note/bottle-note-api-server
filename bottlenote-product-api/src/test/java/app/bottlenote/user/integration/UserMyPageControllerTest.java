package app.bottlenote.user.integration;

import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.user.dto.request.MyBottleRequest;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.PicksMyBottleItem;
import app.bottlenote.user.fixture.UserTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] myPage")
class UserMyPageControllerTest extends IntegrationTestSupport {
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("다른 사용자의 찜 목록을 조회할 때 통했찜 여부가 올바르게 표시된다")
  void 통했찜_여부_표시_테스트() throws Exception {
    // 공통 설정
    var 테스트_술 = alcoholTestFactory.persistAlcohol();
    var 찜한_사용자 = userTestFactory.persistUser();
    var 조회하는_사용자 = userTestFactory.persistUser();
    var 찜하기_요청 = new PicksUpdateRequest(테스트_술.getId(), PICK);
    var 조회_요청 = MyBottleRequest.builder().build();

    // 시나리오 1: 찜한 사용자만 찜
    mockMvc.perform(
        put("/api/v1/picks") // ✅ 매번 새로 생성
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(찜하기_요청))
            .header("Authorization", "Bearer " + getToken(찜한_사용자).accessToken())
            .with(csrf()));

    var 통했찜_전 =
        mockMvc
            .perform(
                get("/api/v1/my-page/{userId}/my-bottle/picks", 찜한_사용자.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(조회_요청))
                    .header("Authorization", "Bearer " + getToken(조회하는_사용자).accessToken())
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // 시나리오 2: 조회하는 사용자도 찜
    mockMvc.perform(
        put("/api/v1/picks") // ✅ 매번 새로 생성
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(찜하기_요청))
            .header("Authorization", "Bearer " + getToken(조회하는_사용자).accessToken())
            .with(csrf()));

    var 통했찜_후 =
        mockMvc
            .perform(
                get("/api/v1/my-page/{userId}/my-bottle/picks", 찜한_사용자.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(조회_요청))
                    .header("Authorization", "Bearer " + getToken(조회하는_사용자).accessToken())
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // 검증
    assertThat(extractPicksItem(통했찜_전).isPicked()).isFalse();
    assertThat(extractPicksItem(통했찜_후).isPicked()).isTrue();
  }

  private PicksMyBottleItem extractPicksItem(MvcResult result) throws Exception {
    var response =
        mapper.readValue(result.getResponse().getContentAsString(UTF_8), GlobalResponse.class);
    var myBottleResponse = mapper.convertValue(response.getData(), MyBottleResponse.class);
    return mapper.convertValue(myBottleResponse.myBottleList().getFirst(), PicksMyBottleItem.class);
  }
}
