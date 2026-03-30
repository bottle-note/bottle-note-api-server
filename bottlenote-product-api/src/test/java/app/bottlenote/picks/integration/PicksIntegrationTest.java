package app.bottlenote.picks.integration;

import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static app.bottlenote.picks.constant.PicksStatus.UNPICK;
import static app.bottlenote.picks.dto.response.PicksUpdateResponse.Message.PICKED;
import static app.bottlenote.picks.dto.response.PicksUpdateResponse.Message.UNPICKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.domain.PicksRepository;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] PickController")
class PicksIntegrationTest extends IntegrationTestSupport {

  @Autowired private PicksRepository picksRepository;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;

  @DisplayName("찜을 등록할 수 있다.")
  @Test
  void test_1() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    TokenItem token = getToken(user);

    PicksUpdateRequest picksUpdateRequest = new PicksUpdateRequest(alcohol.getId(), PICK);

    // When
    MvcTestResult result =
        mockMvcTester
            .put()
            .uri("/api/v1/picks")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(picksUpdateRequest))
            .header("Authorization", "Bearer " + token.accessToken())
            .with(csrf())
            .exchange();

    // Then
    PicksUpdateResponse picksUpdateResponse = extractData(result, PicksUpdateResponse.class);
    assertEquals(picksUpdateResponse.message(), PICKED.message());
  }

  @DisplayName("등록한 찜을 해제할 수 있다.")
  @Test
  void test_2() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    TokenItem token = getToken(user);

    PicksUpdateRequest registerPicksRequest = new PicksUpdateRequest(alcohol.getId(), PICK);
    PicksUpdateRequest unregisterPicksRequest = new PicksUpdateRequest(alcohol.getId(), UNPICK);

    // When - 찜 등록
    MvcTestResult registerResult =
        mockMvcTester
            .put()
            .uri("/api/v1/picks")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(registerPicksRequest))
            .header("Authorization", "Bearer " + token.accessToken())
            .with(csrf())
            .exchange();

    registerResult.assertThat().hasStatusOk();

    Picks picks =
        picksRepository.findByAlcoholIdAndUserId(alcohol.getId(), user.getId()).orElse(null);
    assertNotNull(picks);
    assertEquals(PICK, picks.getStatus());

    // When - 찜 해제
    MvcTestResult result =
        mockMvcTester
            .put()
            .uri("/api/v1/picks")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(unregisterPicksRequest))
            .header("Authorization", "Bearer " + token.accessToken())
            .with(csrf())
            .exchange();

    // Then
    PicksUpdateResponse picksUpdateResponse = extractData(result, PicksUpdateResponse.class);

    assertEquals(picksUpdateResponse.message(), UNPICKED.message());
    Picks unPick =
        picksRepository.findByAlcoholIdAndUserId(alcohol.getId(), user.getId()).orElse(null);
    assertNotNull(unPick);
    assertEquals(UNPICK, unPick.getStatus());
  }
}
