package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.facade.MfdsFacade;
import app.bottlenote.mfds.fixture.MfdsTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] 알코올 상세 MFDS 공개 노출")
class AlcoholDetailMfdsIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private MfdsTestFactory mfdsTestFactory;

  @Test
  @DisplayName("노출 대상 수입사에 연결된 신고는 importer 중첩까지 내려준다")
  void 활성_수입사는_importer를_중첩해_내려준다() throws Exception {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    MfdsImporter active =
        mfdsTestFactory.persistImporter("BIZ-ACTIVE", "보틀상사", MfdsImporterAdminStatus.ACTIVE);
    mfdsTestFactory.persistDeclaration(
        "RCNO-ACTIVE",
        MfdsNormalizationStatus.NORMALIZED,
        active.getId(),
        alcohol.getId(),
        "MANUAL");

    JsonNode declarations = fetchDeclarations(alcohol.getId());

    assertThat(declarations).hasSize(1);
    JsonNode importer = declarations.get(0).path("importer");
    assertThat(importer.isMissingNode()).isFalse();
    assertThat(importer.path("id").asLong()).isEqualTo(active.getId());
    assertThat(importer.path("businessName").asText()).isEqualTo("보틀상사");
  }

  @Test
  @DisplayName("노출에서 제외된 INACTIVE 수입사에 연결된 신고는 목록에 남기고 importer만 생략한다")
  void 비활성_수입사는_importer를_생략한다() throws Exception {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    MfdsImporter inactive =
        mfdsTestFactory.persistImporter("BIZ-INACTIVE", "폐업상사", MfdsImporterAdminStatus.INACTIVE);
    mfdsTestFactory.persistDeclaration(
        "RCNO-INACTIVE",
        MfdsNormalizationStatus.NORMALIZED,
        inactive.getId(),
        alcohol.getId(),
        "MANUAL");

    JsonNode declarations = fetchDeclarations(alcohol.getId());

    assertThat(declarations).hasSize(1);
    JsonNode declaration = declarations.get(0);
    assertThat(declaration.path("rcno").asText()).isEqualTo("RCNO-INACTIVE");
    assertThat(declaration.has("importer")).as("노출에서 제외된 수입사 정보는 응답에 담기지 않아야 한다").isFalse();
    assertThat(declaration.toString()).doesNotContain("폐업상사");
  }

  @Test
  @DisplayName("검증 완료 신고가 상한을 넘으면 최신 순 최대 20건만 내려준다")
  void 상한을_넘으면_최신_20건만_내려준다() throws Exception {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    List<MfdsDeclaration> saved = new ArrayList<>();
    for (int index = 1; index <= 25; index++) {
      saved.add(
          mfdsTestFactory.persistDeclaration(
              String.format("RCNO-%03d", index),
              MfdsNormalizationStatus.NORMALIZED,
              null,
              alcohol.getId(),
              "MANUAL"));
    }
    List<String> latestRcnoDesc =
        saved.reversed().stream()
            .limit(MfdsFacade.MAX_PUBLIC_DECLARATIONS)
            .map(MfdsDeclaration::getRcno)
            .toList();

    JsonNode declarations = fetchDeclarations(alcohol.getId());

    assertThat(declarations).hasSize(MfdsFacade.MAX_PUBLIC_DECLARATIONS);
    List<String> responseRcno = new ArrayList<>();
    declarations.forEach(node -> responseRcno.add(node.path("rcno").asText()));
    assertThat(responseRcno).containsExactlyElementsOf(latestRcnoDesc);
  }

  private JsonNode fetchDeclarations(Long alcoholId) throws Exception {
    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/alcohols/{alcoholId}", alcoholId)
            .contentType(APPLICATION_JSON)
            .exchange();

    result.assertThat().hasStatusOk();
    JsonNode root = mapper.readTree(result.getResponse().getContentAsString());
    JsonNode declarations = root.path("data").path("mfdsDeclarations");
    assertThat(declarations.isArray()).isTrue();
    return declarations;
  }
}
