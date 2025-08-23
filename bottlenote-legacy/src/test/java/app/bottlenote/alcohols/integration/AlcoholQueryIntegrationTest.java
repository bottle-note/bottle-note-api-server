package app.bottlenote.alcohols.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.KeywordTagMapping;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.AlcoholsTastingTags;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("[integration] [controller] AlcoholQuery")
class AlcoholQueryIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholQueryRepository alcoholQueryRepository;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;

  @Test
  @DisplayName("알코올 목록조회를 할 수 있다.")
  void test_1() throws Exception {
    alcoholTestFactory.persistAlcohols(10);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse alcoholSearchResponse =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    List<Alcohol> alcohols = alcoholQueryRepository.findAll();
    assertNotNull(alcoholSearchResponse);
    assertEquals(alcohols.size(), alcoholSearchResponse.getTotalCount());
  }

  @Test
  @DisplayName("키워드를 알코올 목록조회를 할 수 있다.")
  void test_1_1() throws Exception {
    List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(1);
    Alcohol alcohol = alcohols.getFirst();
    TastingTag tag = TastingTag.builder().korName("테스트 태그").engName("test-tag").build();
    alcoholTestFactory.appendTastingTag(alcohol, tag);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "테스트 태그")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);
    List<AlcoholsSearchItem> responseAlcohols = responseData.getAlcohols();

    assertNotNull(responseData);
    assertNotNull(responseAlcohols);

    Long alcoholId = responseAlcohols.getFirst().getAlcoholId();
    Set<AlcoholsTastingTags> alcoholTastingTags =
        alcoholTestFactory.getAlcoholTastingTags(alcoholId);
    List<TastingTag> tagList =
        alcoholTastingTags.stream().map(AlcoholsTastingTags::getTastingTag).toList();

    assertEquals(1, tagList.size());
    assertTrue(tagList.contains(tag));
  }

  @Test
  @DisplayName("키워드를 알코올 목록조회를 할 수 있다.")
  void test_1_1_1() throws Exception {
    // given: 알코올 생성
    List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(1);
    Alcohol alcohol = alcohols.getFirst();

    // 봄 추천 위스키 태그 매핑 설정
    alcoholTestFactory.appendTagsFromKeywordMapping(
        alcohol.getId(), KeywordTagMapping.SPRING_WHISKEY);

    // when: 봄 추천 위스키로 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "봄 추천 위스키")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then: 검색 결과 검증
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);
    List<AlcoholsSearchItem> responseAlcohols = responseData.getAlcohols();

    assertNotNull(responseData);
    assertNotNull(responseAlcohols);
    assertEquals(alcohol.getId(), responseAlcohols.getFirst().getAlcoholId());

    // 조회된 알코올의 태그 검증
    Long alcoholId = responseAlcohols.getFirst().getAlcoholId();
    Set<AlcoholsTastingTags> alcoholTastingTags =
        alcoholTestFactory.getAlcoholTastingTags(alcoholId);
    List<Long> actualTagIds =
        alcoholTastingTags.stream().map(att -> att.getTastingTag().getId()).toList();

    List<Long> expectedTagIds = KeywordTagMapping.SPRING_WHISKEY.getIncludeTags();

    // 조회된 태그들이 SPRING_WHISKEY의 포함 태그와 정확히 일치하는지 확인
    assertEquals(expectedTagIds.size(), actualTagIds.size());
    assertTrue(expectedTagIds.containsAll(actualTagIds));
  }

  @Test
  @DisplayName("알코올 상세 조회를 할 수 있다.")
  void test_2() throws Exception {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/{alcoholId}", alcohol.getId())
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholDetailResponse alcoholDetail =
        mapper.convertValue(response.getData(), AlcoholDetailResponse.class);

    assertNotNull(alcoholDetail.alcohols());
    assertNotNull(alcoholDetail.reviewInfo());
    assertNotNull(alcoholDetail.friendsInfo());
  }

  @Test
  @DisplayName("알코올 상세 조회 시 해당 알콤올을 마셔본 팔로잉 유저의 정보를 조회할 수 있다.")
  void test_3() throws Exception {
    // given
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    final Long alcoholId = alcohol.getId();

    String tokenString = getToken();
    Long currentUserId = getTokenUserId();
    User follower = userTestFactory.persistUser("follower@example.com", "follower");

    userTestFactory.persistFollow(currentUserId, follower.getId());
    userTestFactory.persistFollow(follower.getId(), currentUserId);
    ratingTestFactory.persistRating(currentUserId, alcohol.getId(), 3);
    ratingTestFactory.persistRating(follower.getId(), alcohol.getId(), 4);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/{alcoholId}", alcoholId)
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + tokenString)
                    .with(csrf()))
            .andDo(print())
            .andReturn();
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholDetailResponse alcoholDetail =
        mapper.convertValue(response.getData(), AlcoholDetailResponse.class);

    // then
    assertNotNull(alcoholDetail.friendsInfo());
    assertEquals(1, alcoholDetail.friendsInfo().getFollowerCount());
    assertEquals(follower.getId(), alcoholDetail.friendsInfo().getFriends().getFirst().userId());
  }
}
