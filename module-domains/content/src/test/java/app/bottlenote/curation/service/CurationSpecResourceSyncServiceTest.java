package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.dto.response.CurationSpecSyncResponse;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import app.bottlenote.curation.support.CurationSpecFingerprint;
import app.bottlenote.curation.support.CurationSpecResourceReader;
import app.bottlenote.curation.support.CurationSpecResourceReader.CurationSpecResourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            curationSpecRepository,
            resourceReader,
            new CurationPayloadValidator(objectMapper),
            new CurationSpecFingerprint(objectMapper));
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

  @Test
  @DisplayName("최초 생성 시에는 큐레이션이 없으므로 변경 스펙으로 잡지 않는다")
  void sync_최초_생성은_변경으로_보지_않는다() {
    CurationSpecSyncResponse result = newService(new InMemoryCurationSpecRepository()).sync();

    assertThat(result.createdCount()).isPositive();
    assertThat(result.changedSpecIds()).isEmpty();
    assertThat(result.hasChangedSpecs()).isFalse();
  }

  @Test
  @DisplayName("responseSpec이 그대로면 재동기화해도 변경 스펙이 없다")
  void sync_responseSpec_동일하면_변경_없음() {
    InMemoryCurationSpecRepository repository = new InMemoryCurationSpecRepository();
    CurationSpecResourceSyncService service = newService(repository);
    service.sync();

    CurationSpecSyncResponse result = service.sync();

    assertThat(result.updatedCount()).isPositive();
    assertThat(result.changedSpecIds()).isEmpty();
  }

  @Test
  @DisplayName("responseSpec이 바뀐 스펙만 변경 목록에 담긴다")
  void sync_responseSpec_바뀐_스펙만_담긴다() {
    InMemoryCurationSpecRepository repository = new InMemoryCurationSpecRepository();
    CurationSpecResourceSyncService service = newService(repository);
    service.sync();
    CurationSpec target = repository.findByCode("RECOMMENDED_WHISKY").orElseThrow();
    Map<String, Object> tampered = new LinkedHashMap<>(target.getResponseSpec());
    tampered.put("x-tampered", true);
    target.update(
        target.getName(),
        target.getDescription(),
        target.getRequestSpec(),
        tampered,
        target.getHydratorKey(),
        target.getVersion(),
        true);

    CurationSpecSyncResponse result = service.sync();

    assertThat(result.changedSpecIds()).containsExactly(target.getId());
  }

  @Test
  @DisplayName("키 순서와 수치 표기만 다른 responseSpec은 동일하게 판정한다")
  void sync_키순서와_수치표기_차이는_동일하다() {
    InMemoryCurationSpecRepository repository = new InMemoryCurationSpecRepository();
    CurationSpecResourceSyncService service = newService(repository);
    service.sync();
    CurationSpec target = repository.findByCode("RECOMMENDED_WHISKY").orElseThrow();
    target.update(
        target.getName(),
        target.getDescription(),
        target.getRequestSpec(),
        reorderedCopy(target.getResponseSpec()),
        target.getHydratorKey(),
        target.getVersion(),
        true);

    CurationSpecSyncResponse result = service.sync();

    assertThat(result.changedSpecIds()).isEmpty();
  }

  // 키를 역순으로 다시 담고 정수를 실수 표기로 바꾼다. MySQL·Jackson 왕복에서 생기는 흔들림을 흉내낸다.
  private static Map<String, Object> reorderedCopy(Map<String, Object> source) {
    List<String> keys = new ArrayList<>(source.keySet());
    Collections.reverse(keys);
    Map<String, Object> copy = new LinkedHashMap<>();
    for (String key : keys) {
      Object value = source.get(key);
      if (value instanceof Map<?, ?> nested) {
        @SuppressWarnings("unchecked")
        Map<String, Object> casted = (Map<String, Object>) nested;
        copy.put(key, reorderedCopy(casted));
      } else if (value instanceof Integer intValue) {
        copy.put(key, intValue.doubleValue());
      } else {
        copy.put(key, value);
      }
    }
    return copy;
  }

  private static CurationSpecResourceSyncService newService(
      InMemoryCurationSpecRepository repository) {
    ObjectMapper objectMapper = new ObjectMapper();
    return new CurationSpecResourceSyncService(
        repository,
        new CurationSpecResourceReader(new PathMatchingResourcePatternResolver(), objectMapper),
        new CurationPayloadValidator(objectMapper),
        new CurationSpecFingerprint(objectMapper));
  }
}
