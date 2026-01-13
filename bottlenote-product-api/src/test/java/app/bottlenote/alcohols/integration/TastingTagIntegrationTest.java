package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.fixture.AlcoholMetadataTestFactory;
import app.bottlenote.alcohols.service.TastingTagService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] TastingTag")
class TastingTagIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholMetadataTestFactory metadataTestFactory;
  @Autowired private TastingTagService tastingTagService;

  @BeforeEach
  void setUp() {
    metadataTestFactory.persistDefaultTastingTags();
    tastingTagService.initializeTrie();
  }

  @Test
  @DisplayName("문장에서 태그를 추출할 수 있다")
  void extractTags() throws Exception {
    // given
    String text = "바닐라 향이 좋고 꿀 같은 단맛이 느껴져요";

    // when
    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/tasting-tags/extract")
            .param("text", text)
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + getToken())
            .with(csrf())
            .exchange();

    // then
    List<String> tags = extractDataAsList(result);
    assertThat(tags).containsExactlyInAnyOrder("바닐라", "꿀");
  }

  @Test
  @DisplayName("여러 태그가 포함된 문장에서 모든 태그를 추출한다")
  void extractMultipleTags() throws Exception {
    // given
    String text = "스모키 하면서 피트 향이 강하고 오크 통 숙성의 깊은 맛";

    // when
    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/tasting-tags/extract")
            .param("text", text)
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + getToken())
            .with(csrf())
            .exchange();

    // then
    List<String> tags = extractDataAsList(result);
    assertThat(tags).containsExactlyInAnyOrder("스모키", "피트", "오크");
  }

  @Test
  @DisplayName("부분 매칭은 제외된다")
  void excludePartialMatch() throws Exception {
    // given
    String text = "바닐라빈 향이 좋고 꿀물처럼 달콤해요";

    // when
    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/tasting-tags/extract")
            .param("text", text)
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + getToken())
            .with(csrf())
            .exchange();

    // then
    List<String> tags = extractDataAsList(result);
    assertThat(tags).isEmpty();
  }

  @Test
  @DisplayName("매칭되는 태그가 없으면 빈 리스트를 반환한다")
  void returnEmptyWhenNoMatch() throws Exception {
    // given
    String text = "그냥 평범한 위스키입니다";

    // when
    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/tasting-tags/extract")
            .param("text", text)
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + getToken())
            .with(csrf())
            .exchange();

    // then
    List<String> tags = extractDataAsList(result);
    assertThat(tags).isEmpty();
  }

  private List<String> extractDataAsList(MvcTestResult result) throws Exception {
    result.assertThat().hasStatusOk();
    String responseString = result.getResponse().getContentAsString();
    var response = mapper.readTree(responseString);
    return mapper.convertValue(response.get("data"), new TypeReference<>() {});
  }
}
