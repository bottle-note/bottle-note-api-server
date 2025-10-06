package app.bottlenote.alcohols.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import app.bottlenote.IntegrationTestSupport;
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

  @Test
  @DisplayName("띄어쓰기가 있는 알코올 이름을 띄어쓰기 없이 검색할 수 있다.")
  void test_4() throws Exception {
    // given - 띄어쓰기가 있는 알코올 생성
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("럼 릭", "Rum Rick");

    // when - 띄어쓰기 없이 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "럼릭")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    assertNotNull(responseData);
    assertEquals(1, responseData.getTotalCount());
    assertEquals(alcohol.getId(), responseData.getAlcohols().getFirst().getAlcoholId());
    assertEquals("럼 릭", responseData.getAlcohols().getFirst().getKorName());
  }

  @Test
  @DisplayName("띄어쓰기가 없는 알코올 이름을 띄어쓰기와 함께 검색할 수 있다.")
  void test_5() throws Exception {
    // given - 띄어쓰기가 없는 알코올 생성
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("럼릭", "RumRick");

    // when - 띄어쓰기와 함께 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "럼 릭")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    assertNotNull(responseData);
    assertEquals(1, responseData.getTotalCount());
    assertEquals(alcohol.getId(), responseData.getAlcohols().getFirst().getAlcoholId());
    assertEquals("럼릭", responseData.getAlcohols().getFirst().getKorName());
  }

  @Test
  @DisplayName("영어 알코올 이름도 띄어쓰기 무시 검색이 가능하다.")
  void test_6() throws Exception {
    // given - 영어 이름에 띄어쓰기가 있는 알코올 생성
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("위스키", "Jack Daniels");

    // when - 띄어쓰기 없이 영어로 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "JackDaniels")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    assertNotNull(responseData);
    assertEquals(1, responseData.getTotalCount());
    assertEquals(alcohol.getId(), responseData.getAlcohols().getFirst().getAlcoholId());
    assertEquals("Jack Daniels", responseData.getAlcohols().getFirst().getEngName());
  }

  @Test
  @DisplayName("테이스팅 태그도 띄어쓰기 무시 검색이 가능하다.")
  void test_7() throws Exception {
    // given - 띄어쓰기가 있는 테이스팅 태그를 가진 알코올 생성
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    TastingTag tag = TastingTag.builder().korName("과일 향").engName("fruity aroma").build();
    alcoholTestFactory.appendTastingTag(alcohol, tag);

    // when - 띄어쓰기 없이 태그로 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "과일향")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    assertNotNull(responseData);
    assertEquals(1, responseData.getTotalCount());
    assertEquals(alcohol.getId(), responseData.getAlcohols().getFirst().getAlcoholId());
  }

  @Test
  @DisplayName("단어 순서를 바꿔서 검색할 수 있다 - 글래드스톤 엑스를 엑스 글래드스톤으로 검색")
  void test_8() throws Exception {
    // given - "글래드스톤 엑스" 알코올 생성
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("글래드스톤 엑스", "Gladstone X");

    // when - 순서를 바꿔서 "엑스 글래드스톤"으로 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "엑스 글래드스톤")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    assertNotNull(responseData);
    assertEquals(1, responseData.getTotalCount());
    assertEquals(alcohol.getId(), responseData.getAlcohols().getFirst().getAlcoholId());
    assertEquals("글래드스톤 엑스", responseData.getAlcohols().getFirst().getKorName());
  }

  @Test
  @DisplayName("영어 이름도 단어 순서를 바꿔서 검색할 수 있다 - Jack Daniels를 Daniels Jack으로 검색")
  void test_9() throws Exception {
    // given - "Jack Daniels" 알코올 생성
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("잭 다니엘스", "Jack Daniels");

    // when - 순서를 바꿔서 "Daniels Jack"으로 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "Daniels Jack")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    assertNotNull(responseData);
    assertEquals(1, responseData.getTotalCount());
    assertEquals(alcohol.getId(), responseData.getAlcohols().getFirst().getAlcoholId());
    assertEquals("Jack Daniels", responseData.getAlcohols().getFirst().getEngName());
  }

  @Test
  @DisplayName("세 단어 이상도 순서 무관하게 검색할 수 있다")
  void test_10() throws Exception {
    // given - 세 단어로 구성된 알코올 생성
    Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("조니 워커 블랙", "Johnny Walker Black");

    // when - 순서를 바꿔서 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "블랙 조니 워커")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    assertNotNull(responseData);
    assertEquals(1, responseData.getTotalCount());
    assertEquals(alcohol.getId(), responseData.getAlcohols().getFirst().getAlcoholId());
    assertEquals("조니 워커 블랙", responseData.getAlcohols().getFirst().getKorName());
  }

  @Test
  @DisplayName("일부 단어만 입력해도 검색할 수 있다")
  void test_11() throws Exception {
    // given - "조니 워커 블랙 라벨" 알코올 생성
    Alcohol alcohol =
        alcoholTestFactory.persistAlcoholWithName("조니 워커 블랙 라벨", "Johnny Walker Black Label");

    // when - "조니 블랙"으로만 검색 (워커, 라벨 생략)
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/search")
                    .param("keyword", "조니 블랙")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    AlcoholSearchResponse responseData =
        mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

    assertNotNull(responseData);
    assertEquals(1, responseData.getTotalCount());
    assertEquals(alcohol.getId(), responseData.getAlcohols().getFirst().getAlcoholId());
    assertEquals("조니 워커 블랙 라벨", responseData.getAlcohols().getFirst().getKorName());
  }
}
