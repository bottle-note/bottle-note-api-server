package app.bottlenote.alcohols.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("[integration] [controller] AlcoholExplore")
class AlcoholExploreIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;

  @Test
  @DisplayName("위스키 둘러보기를 할 수 있다")
  void test_1() throws Exception {
    // given
    alcoholTestFactory.persistAlcohols(30);

    // when
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/explore/standard")
                    .param("size", "20")
                    .param("cursor", "0")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getData();
    Long totalCount = ((Number) data.get("totalCount")).longValue();

    assertNotNull(data);
    assertEquals(30L, totalCount);

    @SuppressWarnings("unchecked")
    Map<String, Object> cursorResponseData = (Map<String, Object>) data.get("cursorResponse");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) cursorResponseData.get("items");

    assertNotNull(items);
    assertEquals(20, items.size());
  }

  @Test
  @DisplayName("위스키 둘러보기에서 페이징이 동작한다")
  void test_2() throws Exception {
    // given
    alcoholTestFactory.persistAlcohols(30);

    // when - 첫 페이지
    MvcResult result1 =
        mockMvc
            .perform(
                get("/api/v1/alcohols/explore/standard")
                    .param("size", "10")
                    .param("cursor", "0")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // when - 두 번째 페이지
    MvcResult result2 =
        mockMvc
            .perform(
                get("/api/v1/alcohols/explore/standard")
                    .param("size", "10")
                    .param("cursor", "10")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String response1String = result1.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse globalResponse1 = mapper.readValue(response1String, GlobalResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> data1 = (Map<String, Object>) globalResponse1.getData();

    @SuppressWarnings("unchecked")
    Map<String, Object> cursorResponse1 = (Map<String, Object>) data1.get("cursorResponse");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items1 = (List<Map<String, Object>>) cursorResponse1.get("items");

    String response2String = result2.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse globalResponse2 = mapper.readValue(response2String, GlobalResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> data2 = (Map<String, Object>) globalResponse2.getData();

    @SuppressWarnings("unchecked")
    Map<String, Object> cursorResponse2 = (Map<String, Object>) data2.get("cursorResponse");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items2 = (List<Map<String, Object>>) cursorResponse2.get("items");

    assertNotNull(items1);
    assertNotNull(items2);
    assertEquals(10, items1.size());
    assertEquals(10, items2.size());

    // 두 페이지의 결과는 일관성 있게 정렬되어야 함 (같은 cursor에서는 같은 결과)
    Set<Long> ids1 = new HashSet<>();
    Set<Long> ids2 = new HashSet<>();

    items1.forEach(item -> ids1.add(((Number) item.get("alcoholId")).longValue()));
    items2.forEach(item -> ids2.add(((Number) item.get("alcoholId")).longValue()));

    // 두 페이지에 중복이 없어야 함
    assertTrue(java.util.Collections.disjoint(ids1, ids2), "페이지 간 결과가 중복되면 안됩니다");
  }

  @Test
  @DisplayName("위스키 둘러보기에서 키워드로 필터링할 수 있다")
  void test_3() throws Exception {
    // given
    alcoholTestFactory.persistAlcohols(10);
    alcoholTestFactory.persistAlcoholWithName("글렌피딕 12년", "Glenfiddich 12");
    alcoholTestFactory.persistAlcoholWithName("글렌리벳 18년", "Glenlivet 18");

    // when - "글렌" 키워드로 검색
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/alcohols/explore/standard")
                    .param("keywords", "글렌")
                    .param("size", "20")
                    .param("cursor", "0")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andReturn();

    // then
    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getData();
    Long totalCount = ((Number) data.get("totalCount")).longValue();

    @SuppressWarnings("unchecked")
    Map<String, Object> cursorResponseData = (Map<String, Object>) data.get("cursorResponse");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) cursorResponseData.get("items");

    assertNotNull(items);
    assertEquals(2L, totalCount);
    assertEquals(2, items.size());

    // 결과에 "글렌"이 포함되어 있는지 확인
    items.forEach(
        item -> {
          String korName = (String) item.get("korName");
          assertTrue(korName.contains("글렌"), "결과에 '글렌'이 포함되어야 합니다");
        });
  }

  @Test
  @DisplayName("같은 cursor 값으로 여러 번 조회하면 일관된 결과를 반환한다")
  void test_4() throws Exception {
    // given
    alcoholTestFactory.persistAlcohols(30);

    // when - 같은 cursor로 두 번 조회
    MvcResult result1 =
        mockMvc
            .perform(
                get("/api/v1/alcohols/explore/standard")
                    .param("size", "10")
                    .param("cursor", "5")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andReturn();

    MvcResult result2 =
        mockMvc
            .perform(
                get("/api/v1/alcohols/explore/standard")
                    .param("size", "10")
                    .param("cursor", "5")
                    .contentType(APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andReturn();

    // then
    String response1String = result1.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse globalResponse1 = mapper.readValue(response1String, GlobalResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> data1 = (Map<String, Object>) globalResponse1.getData();

    @SuppressWarnings("unchecked")
    Map<String, Object> cursorResponse1 = (Map<String, Object>) data1.get("cursorResponse");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items1 = (List<Map<String, Object>>) cursorResponse1.get("items");

    String response2String = result2.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse globalResponse2 = mapper.readValue(response2String, GlobalResponse.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> data2 = (Map<String, Object>) globalResponse2.getData();

    @SuppressWarnings("unchecked")
    Map<String, Object> cursorResponse2 = (Map<String, Object>) data2.get("cursorResponse");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items2 = (List<Map<String, Object>>) cursorResponse2.get("items");

    // 같은 cursor에서는 같은 결과를 반환해야 함
    assertEquals(items1.size(), items2.size());
    for (int i = 0; i < items1.size(); i++) {
      Long id1 = ((Number) items1.get(i).get("alcoholId")).longValue();
      Long id2 = ((Number) items2.get(i).get("alcoholId")).longValue();
      assertEquals(id1, id2, "같은 cursor에서는 일관된 순서의 결과를 반환해야 합니다");
    }
  }
}
