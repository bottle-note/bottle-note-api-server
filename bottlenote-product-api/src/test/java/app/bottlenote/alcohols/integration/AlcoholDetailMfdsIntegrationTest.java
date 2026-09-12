package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.fixture.MfdsTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] [controller] 알코올 상세는 식약처 수입 신고를 포함하지 않는다")
class AlcoholDetailMfdsIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private MfdsTestFactory mfdsTestFactory;

  @Test
  @DisplayName("매칭·정규화가 끝난 신고가 있어도 상세 JSON에 mfdsDeclarations를 넣지 않는다")
  void 상세_응답에_수입_신고를_넣지_않는다() throws Exception {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    var importer =
        mfdsTestFactory.persistImporter("BIZ-ACTIVE", "보틀상사", MfdsImporterAdminStatus.ACTIVE);
    mfdsTestFactory.persistDeclaration(
        "RCNO-ACTIVE",
        MfdsNormalizationStatus.NORMALIZED,
        importer.getId(),
        alcohol.getId(),
        "MANUAL");

    var result =
        mockMvcTester
            .get()
            .uri("/api/v1/alcohols/{alcoholId}", alcohol.getId())
            .contentType(APPLICATION_JSON)
            .exchange();

    result.assertThat().hasStatusOk();
    JsonNode data = mapper.readTree(result.getResponse().getContentAsString()).path("data");
    assertThat(data.has("mfdsDeclarations")).isFalse();
    assertThat(data.toString()).doesNotContain("RCNO-ACTIVE", "보틀상사");
  }
}
