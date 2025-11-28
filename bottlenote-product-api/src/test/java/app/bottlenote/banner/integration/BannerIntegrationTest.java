package app.bottlenote.banner.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.banner.dto.response.BannerResponse;
import app.bottlenote.banner.fixture.BannerTestFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] BannerQueryController")
@WithMockUser
class BannerIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private BannerTestFactory bannerTestFactory;

  @Nested
  @DisplayName("배너 목록 조회")
  class GetActiveBanners {

    @DisplayName("활성 배너 목록을 조회할 수 있다.")
    @Test
    void test_1() throws Exception {
      // given
      bannerTestFactory.persistMultipleBanners(5);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/banners")
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(5));
    }

    @DisplayName("limit 파라미터로 조회 개수를 제한할 수 있다.")
    @Test
    void test_2() throws Exception {
      // given
      bannerTestFactory.persistMultipleBanners(5);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/banners")
                  .param("limit", "3")
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(3));
    }

    @DisplayName("배너는 sortOrder 기준으로 오름차순 정렬된다.")
    @Test
    void test_3() throws Exception {
      // given
      bannerTestFactory.persistBanner("배너3", "https://example.com/3.jpg",
          app.bottlenote.banner.constant.TextPosition.CENTER,
          app.bottlenote.banner.constant.BannerType.CURATION, 3, true);
      bannerTestFactory.persistBanner("배너1", "https://example.com/1.jpg",
          app.bottlenote.banner.constant.TextPosition.CENTER,
          app.bottlenote.banner.constant.BannerType.CURATION, 1, true);
      bannerTestFactory.persistBanner("배너2", "https://example.com/2.jpg",
          app.bottlenote.banner.constant.TextPosition.CENTER,
          app.bottlenote.banner.constant.BannerType.CURATION, 2, true);

      // when
      MvcResult result = mockMvc
          .perform(
              get("/api/v1/banners")
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          .andExpect(status().isOk())
          .andReturn();

      // then
      List<BannerResponse> banners = extractDataAsList(result, new TypeReference<>() {});
      assertEquals(3, banners.size());
      assertEquals(1, banners.get(0).getSortOrder());
      assertEquals(2, banners.get(1).getSortOrder());
      assertEquals(3, banners.get(2).getSortOrder());
    }

    @DisplayName("비활성 배너는 조회되지 않는다.")
    @Test
    void test_4() throws Exception {
      // given
      bannerTestFactory.persistMixedActiveBanners(3, 2);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/banners")
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(3));
    }

    @DisplayName("활성 배너가 없으면 빈 배열을 반환한다.")
    @Test
    void test_5() throws Exception {
      // given - 데이터 없음

      // when & then
      MvcResult result = mockMvc
          .perform(
              get("/api/v1/banners")
                  .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").isArray())
          .andReturn();

      List<BannerResponse> banners = extractDataAsList(result, new TypeReference<>() {});
      assertTrue(banners.isEmpty());
    }
  }

  private <T> List<T> extractDataAsList(MvcResult result, TypeReference<List<T>> typeRef) throws Exception {
    String responseString = result.getResponse().getContentAsString();
    var response = mapper.readValue(responseString, app.bottlenote.global.data.response.GlobalResponse.class);
    return mapper.convertValue(response.getData(), typeRef);
  }
}
