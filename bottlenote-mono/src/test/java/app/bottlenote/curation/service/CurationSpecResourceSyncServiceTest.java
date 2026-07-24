package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.dto.response.CurationSpecSyncResponse;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import app.bottlenote.curation.support.CurationSpecResourceReader;
import app.bottlenote.curation.support.CurationSpecResourceReader.CurationSpecResourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Tag("unit")
@DisplayName("CurationSpecResourceSyncService 단위 테스트")
class CurationSpecResourceSyncServiceTest {

  @Test
  @DisplayName("리소스 OpenAPI 스펙을 curation_spec으로 생성하고 재실행 시 갱신한다")
  void sync_리소스_스펙_생성_및_갱신() {
    InMemoryCurationSpecRepository curationSpecRepository = new InMemoryCurationSpecRepository();
    ObjectMapper objectMapper = new ObjectMapper();
    CurationSpecResourceReader resourceReader =
        new CurationSpecResourceReader(new PathMatchingResourcePatternResolver(), objectMapper);
    CurationSpecResourceSyncService service =
        new CurationSpecResourceSyncService(
            curationSpecRepository, resourceReader, new CurationPayloadValidator(objectMapper));
    List<CurationSpecResourceDocument> specDocuments = resourceReader.readAll();
    List<String> specCodes =
        specDocuments.stream().map(CurationSpecResourceDocument::code).toList();

    CurationSpecSyncResponse firstResult = service.sync();

    assertThat(specDocuments).isNotEmpty();
    assertThat(firstResult.createdCount()).isEqualTo(specDocuments.size());
    assertThat(firstResult.updatedCount()).isZero();
    assertThat(curationSpecRepository.findAllByIsActiveTrueOrderByIdAsc())
        .hasSize(specDocuments.size());
    assertThat(
            curationSpecRepository.findAllByIsActiveTrueOrderByIdAsc().stream()
                .map(CurationSpec::getCode)
                .toList())
        .containsExactlyInAnyOrderElementsOf(specCodes);

    CurationSpec recommended =
        curationSpecRepository.findByCode("RECOMMENDED_WHISKY").orElseThrow();
    assertThat(recommended.getVersion()).isEqualTo(2);
    assertThat(recommended.getRequestSpec()).containsKey("required");
    assertThat(recommended.getResponseSpec().toString()).contains("x-graphql", "stats");

    CurationSpecSyncResponse secondResult = service.sync();

    assertThat(secondResult.createdCount()).isZero();
    assertThat(secondResult.updatedCount()).isEqualTo(specDocuments.size());
    assertThat(curationSpecRepository.findAllByIsActiveTrueOrderByIdAsc())
        .extracting(CurationSpec::getCode)
        .containsExactlyElementsOf(specCodes);
  }
}
