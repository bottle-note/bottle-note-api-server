package app.bottlenote.mfds.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.fixture.MfdsTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] [controller] Product 수입 정보 공개 조회")
class MfdsPublicQueryIntegrationTest extends IntegrationTestSupport {

  @Autowired private MfdsTestFactory mfdsTestFactory;

  @Test
  @DisplayName("인증 없이 수입 주류 목록과 상세를 조회한다")
  void 인증_없이_수입_주류를_조회한다() throws Exception {
    MfdsImporter importer =
        mfdsTestFactory.persistImporter("BIZ-PUB", "보틀상사", MfdsImporterAdminStatus.ACTIVE);
    MfdsDeclaration declaration =
        mfdsTestFactory.persistPublicDeclaration(
            "RCNO-PUB-1",
            importer.getId(),
            "보틀상사",
            "글렌피딕 15년",
            LocalDate.of(2026, 8, 20),
            "GB",
            "영국");

    var list =
        mockMvcTester
            .get()
            .uri("/api/v1/mfds/alcohols?alcoholNameKo={name}", "글렌피딕 15년")
            .accept(APPLICATION_JSON)
            .exchange();
    list.assertThat().hasStatusOk();
    JsonNode items = mapper.readTree(list.getResponse().getContentAsString()).path("data");
    assertThat(items.isArray()).isTrue();
    assertThat(items.get(0).path("rcno").asText()).isEqualTo("RCNO-PUB-1");
    assertThat(items.get(0).path("processedDate").asText()).isEqualTo("2026-08-20");

    var detail =
        mockMvcTester
            .get()
            .uri("/api/v1/mfds/alcohols/{id}", declaration.getId())
            .accept(APPLICATION_JSON)
            .exchange();
    detail.assertThat().hasStatusOk();
    JsonNode data = mapper.readTree(detail.getResponse().getContentAsString()).path("data");
    assertThat(data.path("id").asLong()).isEqualTo(declaration.getId());
    assertThat(data.path("importer").path("businessName").asText()).isEqualTo("보틀상사");
    assertThat(data.has("reviewNote")).isFalse();
  }

  @Test
  @DisplayName("수출국 목록과 수입사 목록을 공개로 조회한다")
  void 국가와_수입사를_공개_조회한다() throws Exception {
    mfdsTestFactory.persistImporter("BIZ-LIST", "윌리엄그랜트", MfdsImporterAdminStatus.ACTIVE);
    mfdsTestFactory.persistPublicDeclaration(
        "RCNO-GB", null, null, "글렌피딕", LocalDate.of(2026, 8, 20), "GB", "영국");

    var countries =
        mockMvcTester.get().uri("/api/v1/mfds/countries").accept(APPLICATION_JSON).exchange();
    countries.assertThat().hasStatusOk();
    JsonNode countryData =
        mapper.readTree(countries.getResponse().getContentAsString()).path("data");
    assertThat(countryData.toString()).contains("GB", "영국");

    var importers =
        mockMvcTester
            .get()
            .uri("/api/v1/mfds/importers?keyword={keyword}", "윌리엄")
            .accept(APPLICATION_JSON)
            .exchange();
    importers.assertThat().hasStatusOk();
    JsonNode importerData =
        mapper.readTree(importers.getResponse().getContentAsString()).path("data");
    assertThat(importerData.toString()).contains("윌리엄그랜트");
  }
}
