package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.dto.response.CurationSpecListResponse;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.exception.CurationExceptionCode;
import app.bottlenote.curation.fixture.CurationFixtureFactory;
import app.bottlenote.curation.fixture.InMemoryCurationExtensionRepository;
import app.bottlenote.curation.fixture.InMemoryCurationRepository;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("CurationSpecQueryService 단위 테스트")
class CurationSpecQueryServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  InMemoryCurationSpecRepository specRepository;
  CurationFixtureFactory fixtureFactory;
  CurationSpecQueryService service;

  @BeforeEach
  void setUp() {
    specRepository = new InMemoryCurationSpecRepository();
    fixtureFactory =
        new CurationFixtureFactory(
            specRepository,
            new InMemoryCurationRepository(),
            new InMemoryCurationExtensionRepository());
    service = new CurationSpecQueryService(specRepository);
  }

  @Test
  @DisplayName("스펙 목록은 requestSpec, responseSpec, hydratorKey 없이 메타만 반환한다")
  void listActiveSpecs_returnsMetaOnly() {
    createSpec("RECOMMENDED_WHISKY", true);
    createSpec("INACTIVE_SPEC", false);

    List<CurationSpecListResponse> result = service.listActiveSpecs();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).code()).isEqualTo("RECOMMENDED_WHISKY");
    var json = OBJECT_MAPPER.valueToTree(result.get(0));
    assertThat(json.has("requestSpec")).isFalse();
    assertThat(json.has("responseSpec")).isFalse();
    assertThat(json.has("hydratorKey")).isFalse();
  }

  @Test
  @DisplayName("스펙 상세는 requestSpec, responseSpec, hydratorKey를 반환한다")
  void getSpecDetail_returnsFullSpec() {
    CurationSpec spec = createSpec("RECOMMENDED_WHISKY", true);

    var result = service.getSpecDetail(spec.getId());

    assertThat(result.hydratorKey()).isEqualTo("alcohol");
    assertThat(result.requestSpec()).containsKey("required");
    assertThat(result.responseSpec()).containsKey("type");
  }

  @Test
  @DisplayName("Product 공개 스펙 상세는 비활성 스펙을 조회하지 않는다")
  void getActiveSpecDetail_whenInactive_throwsNotFound() {
    CurationSpec spec = createSpec("INACTIVE_SPEC", false);

    assertThatThrownBy(() -> service.getActiveSpecDetail(spec.getId()))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", CurationExceptionCode.CURATION_SPEC_NOT_FOUND);
  }

  @Test
  @DisplayName("없는 스펙 상세 조회는 예외가 발생한다")
  void getSpecDetail_whenMissing_throwsNotFound() {
    assertThatThrownBy(() -> service.getSpecDetail(999L))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", CurationExceptionCode.CURATION_SPEC_NOT_FOUND);
  }

  private CurationSpec createSpec(String code, boolean active) {
    CurationSpec spec =
        fixtureFactory.saveSpec(
            code,
            "추천 위스키",
            "추천 설명",
            Map.of("type", "object", "required", List.of("source", "alcohol")),
            Map.of("type", "object"),
            "alcohol",
            1);
    if (!active) {
      spec.update(
          spec.getName(),
          spec.getDescription(),
          spec.getRequestSpec(),
          spec.getResponseSpec(),
          spec.getHydratorKey(),
          spec.getVersion(),
          false);
    }
    return spec;
  }
}
