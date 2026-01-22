package app.bottlenote.history.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.common.fixture.HistoryTestData;
import app.bottlenote.common.fixture.TestDataSetupHelper;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.history.dto.response.UserHistoryItem;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [history] UserHistory")
class UserHistoryIntegrationTest extends IntegrationTestSupport {

  @Autowired private TestDataSetupHelper testDataSetupHelper;

  @DisplayName("파라미터 없이 유저 히스토리를 조회할 수 있다.")
  @Test
  void test_1() throws Exception {
    // given
    HistoryTestData data = testDataSetupHelper.setupHistoryTestData();
    User targetUser = data.getUser(0);
    TokenItem token = getToken(targetUser);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/history/{targetUserId}", targetUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token.accessToken())
                    .with(csrf()))
            .andReturn();

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    System.out.println("contentAsString = " + contentAsString);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);

    UserHistorySearchResponse userHistorySearchResponse =
        mapper.convertValue(response.getData(), UserHistorySearchResponse.class);

    // when & then
    Assertions.assertNotNull(userHistorySearchResponse);
    Assertions.assertNotNull(userHistorySearchResponse.userHistories());
  }

  @DisplayName("유저 히스토리를 정렬해서 조회할 수 있다.")
  @Test
  void test_2() throws Exception {
    // given
    HistoryTestData data = testDataSetupHelper.setupHistoryTestData();
    User targetUser = data.getUser(0);
    TokenItem token = getToken(targetUser);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/history/{targetUserId}", targetUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token.accessToken())
                    .param("sortOrder", SortOrder.DESC.name())
                    .with(csrf()))
            .andReturn();

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);

    UserHistorySearchResponse userHistorySearchResponse =
        mapper.convertValue(response.getData(), UserHistorySearchResponse.class);

    // when & then
    Assertions.assertNotNull(userHistorySearchResponse);
    Assertions.assertNotNull(userHistorySearchResponse.userHistories());
    Assertions.assertFalse(userHistorySearchResponse.userHistories().isEmpty());

    List<UserHistoryItem> userHistories = userHistorySearchResponse.userHistories();
    for (int i = 1; i < userHistories.size(); i++) {
      LocalDateTime current = userHistories.get(i - 1).getCreatedAt();
      LocalDateTime next = userHistories.get(i).getCreatedAt();
      Assertions.assertTrue(current.isAfter(next) || current.isEqual(next));
    }

    Assertions.assertEquals(userHistories.size(), userHistorySearchResponse.totalCount());
  }

  @DisplayName("날짜 검색조건으로 유저 히스토리를 조회할 수 있다.")
  @Test
  void test_3() throws Exception {
    // given
    HistoryTestData data = testDataSetupHelper.setupHistoryTestData();
    User targetUser = data.getUser(0);
    TokenItem token = getToken(targetUser);

    MvcResult initialMvcResult =
        mockMvc
            .perform(
                get("/api/v1/history/{targetUserId}", targetUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token.accessToken())
                    .with(csrf()))
            .andReturn();

    String initialResponseString =
        initialMvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse initialGlobalResponse =
        mapper.readValue(initialResponseString, GlobalResponse.class);

    UserHistorySearchResponse initialUserHistoryResponse =
        mapper.convertValue(initialGlobalResponse.getData(), UserHistorySearchResponse.class);

    List<LocalDateTime> createdAtList =
        initialUserHistoryResponse.userHistories().stream()
            .map(UserHistoryItem::getCreatedAt)
            .sorted()
            .toList();

    MvcResult filteredMvcResult =
        mockMvc
            .perform(
                get("/api/v1/history/{targetUserId}", targetUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token.accessToken())
                    .param("startDate", createdAtList.get(0).toString())
                    .param("endDate", createdAtList.get(1).toString())
                    .with(csrf()))
            .andReturn();

    String filteredResponseString =
        filteredMvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse filteredGlobalResponse =
        mapper.readValue(filteredResponseString, GlobalResponse.class);

    UserHistorySearchResponse filteredUserHistoryResponse =
        mapper.convertValue(filteredGlobalResponse.getData(), UserHistorySearchResponse.class);

    Assertions.assertNotNull(filteredUserHistoryResponse);
    List<UserHistoryItem> userHistoryItems = filteredUserHistoryResponse.userHistories();
    Assertions.assertNotNull(userHistoryItems);
    Assertions.assertFalse(userHistoryItems.isEmpty());

    final LocalDateTime startDate = createdAtList.get(0);
    final LocalDateTime endDate = createdAtList.get(1);

    for (UserHistoryItem historyDetail : userHistoryItems) {
      LocalDateTime createdAt = historyDetail.getCreatedAt();
      Assertions.assertTrue(!createdAt.isBefore(startDate) && !createdAt.isAfter(endDate));
    }
  }

  @DisplayName("별점으로 유저 히스토리 필터링하여 조회할 수 있다.")
  @Test
  void test_4() throws Exception {
    // given
    HistoryTestData data = testDataSetupHelper.setupHistoryTestData();
    User targetUser = data.getUser(0);
    TokenItem token = getToken(targetUser);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/history/{targetUserId}", targetUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token.accessToken())
                    .param("ratingPoint", "5")
                    .with(csrf()))
            .andReturn();

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);

    UserHistorySearchResponse userHistorySearchResponse =
        mapper.convertValue(response.getData(), UserHistorySearchResponse.class);

    // when & then
    Assertions.assertNotNull(userHistorySearchResponse);
    Assertions.assertNotNull(userHistorySearchResponse.userHistories());
  }

  @DisplayName("찜/찜 해제 검색조건으로 유저 히스토리를 조회할 수 있다.")
  @Test
  void test_5() throws Exception {
    // given
    HistoryTestData data = testDataSetupHelper.setupHistoryTestData();
    User targetUser = data.getUser(0);
    TokenItem token = getToken(targetUser);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/history/{targetUserId}", targetUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token.accessToken())
                    .param("picksStatus", PicksStatus.PICK.name())
                    .param("picksStatus", PicksStatus.UNPICK.name())
                    .with(csrf()))
            .andReturn();

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);

    UserHistorySearchResponse userHistorySearchResponse =
        mapper.convertValue(response.getData(), UserHistorySearchResponse.class);

    // when & then
    Assertions.assertNotNull(userHistorySearchResponse);
    Assertions.assertNotNull(userHistorySearchResponse.userHistories());
  }

  @DisplayName("리뷰 필터 조건으로 유저 히스토리를 조회할 수 있다.")
  @Test
  void test_6() throws Exception {
    // given
    HistoryTestData data = testDataSetupHelper.setupHistoryTestData();
    User targetUser = data.getUser(0);
    TokenItem token = getToken(targetUser);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/history/{targetUserId}", targetUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token.accessToken())
                    .param("historyReviewFilterType", "BEST_REVIEW")
                    .param("historyReviewFilterType", "REVIEW_LIKE")
                    .param("historyReviewFilterType", "REVIEW_REPLY")
                    .with(csrf()))
            .andReturn();

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);

    UserHistorySearchResponse userHistorySearchResponse =
        mapper.convertValue(response.getData(), UserHistorySearchResponse.class);

    // when & then
    Assertions.assertNotNull(userHistorySearchResponse);
    Assertions.assertNotNull(userHistorySearchResponse.userHistories());
  }
}
