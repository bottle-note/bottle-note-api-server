package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] Product 알코올 상세 description 계약")
class AlcoholDetailDescriptionIntegrationTest extends IntegrationTestSupport {

  private static final String ENDPOINT = "/api/v1/alcohols/{alcoholId}";

  @Autowired private AlcoholTestFactory alcoholTestFactory;

  @Test
  @DisplayName("저장된 알코올 설명을 상세 응답에 반환한다")
  void 저장된_description을_반환한다() throws Exception {
    Alcohol alcohol =
        alcoholTestFactory.persistAlcohol(Alcohol.builder().description("셰리 캐스크의 깊고 달콤한 풍미"));

    JsonNode alcohols = detailOf(alcohol);

    assertThat(alcohols.path("description").asText()).isEqualTo("셰리 캐스크의 깊고 달콤한 풍미");
  }

  @Test
  @DisplayName("알코올 설명이 없으면 상세 응답에 null을 반환한다")
  void description이_없으면_null을_반환한다() throws Exception {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();

    JsonNode alcohols = detailOf(alcohol);

    assertThat(alcohols.has("description")).isTrue();
    assertThat(alcohols.path("description").isNull()).isTrue();
  }

  private JsonNode detailOf(Alcohol alcohol) throws Exception {
    MvcTestResult result =
        mockMvcTester.get().uri(ENDPOINT, alcohol.getId()).contentType(APPLICATION_JSON).exchange();
    result.assertThat().hasStatusOk();
    return mapper.readTree(result.getResponse().getContentAsString()).at("/data/alcohols");
  }
}
