package app.bottlenote.banner.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.banner.dto.response.BannerResponse;
import app.bottlenote.banner.fixture.BannerTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] BannerQueryController")
@WithMockUser
class BannerIntegrationTest extends IntegrationTestSupport {

  @Autowired private BannerTestFactory bannerTestFactory;

  @Nested
  @DisplayName("배너 목록 조회")
  class GetActiveBanners {

    @DisplayName("활성 배너 목록을 조회할 수 있다.")
    @Test
    void test_1() throws Exception {
      // given
      bannerTestFactory.persistMultipleBanners(5);

      // when
      MvcTestResult result =
          mockMvcTester.get().uri("/api/v1/banners").contentType(APPLICATION_JSON).exchange();

      // then
      List<BannerResponse> banners = extractDataAsList(result, new TypeReference<>() {});
      assertEquals(5, banners.size());
    }

    @DisplayName("limit 파라미터로 조회 개수를 제한할 수 있다.")
    @Test
    void test_2() throws Exception {
      // given
      bannerTestFactory.persistMultipleBanners(5);

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/banners")
              .param("limit", "3")
              .contentType(APPLICATION_JSON)
              .exchange();

      // then
      List<BannerResponse> banners = extractDataAsList(result, new TypeReference<>() {});
      assertEquals(3, banners.size());
    }

    @DisplayName("배너는 sortOrder 기준으로 오름차순 정렬된다.")
    @Test
    void test_3() throws Exception {
      // given
      bannerTestFactory.persistBanner(
          "배너3",
          "https://example.com/3.jpg",
          app.bottlenote.banner.constant.TextPosition.CENTER,
          app.bottlenote.banner.constant.BannerType.CURATION,
          3,
          true);
      bannerTestFactory.persistBanner(
          "배너1",
          "https://example.com/1.jpg",
          app.bottlenote.banner.constant.TextPosition.CENTER,
          app.bottlenote.banner.constant.BannerType.CURATION,
          1,
          true);
      bannerTestFactory.persistBanner(
          "배너2",
          "https://example.com/2.jpg",
          app.bottlenote.banner.constant.TextPosition.CENTER,
          app.bottlenote.banner.constant.BannerType.CURATION,
          2,
          true);

      // when
      MvcTestResult result =
          mockMvcTester.get().uri("/api/v1/banners").contentType(APPLICATION_JSON).exchange();

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

      // when
      MvcTestResult result =
          mockMvcTester.get().uri("/api/v1/banners").contentType(APPLICATION_JSON).exchange();

      // then
      List<BannerResponse> banners = extractDataAsList(result, new TypeReference<>() {});
      assertEquals(3, banners.size());
    }

    @DisplayName("활성 배너가 없으면 빈 배열을 반환한다.")
    @Test
    void test_5() throws Exception {
      // given - 데이터 없음

      // when
      MvcTestResult result =
          mockMvcTester.get().uri("/api/v1/banners").contentType(APPLICATION_JSON).exchange();

      // then
      List<BannerResponse> banners = extractDataAsList(result, new TypeReference<>() {});
      assertTrue(banners.isEmpty());
    }
  }

  private <T> List<T> extractDataAsList(MvcTestResult result, TypeReference<List<T>> typeRef)
      throws Exception {
    result.assertThat().hasStatusOk();
    String responseString = result.getResponse().getContentAsString();
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    return mapper.convertValue(response.getData(), typeRef);
  }
}
