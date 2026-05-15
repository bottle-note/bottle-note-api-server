package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import app.bottlenote.curation.dto.request.CurationSpecCreateRequest;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.exception.CurationExceptionCode;
import app.bottlenote.curation.fixture.InMemoryCurationExtensionRepository;
import app.bottlenote.curation.fixture.InMemoryCurationRepository;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("CurationV2Service 단위 테스트")
class CurationV2ServiceTest {

  InMemoryCurationSpecRepository curationSpecRepository;
  InMemoryCurationRepository curationRepository;
  InMemoryCurationExtensionRepository curationExtensionRepository;
  CurationV2Service curationV2Service;

  @BeforeEach
  void setUp() {
    curationSpecRepository = new InMemoryCurationSpecRepository();
    curationRepository = new InMemoryCurationRepository();
    curationExtensionRepository = new InMemoryCurationExtensionRepository();
    curationV2Service =
        new CurationV2Service(
            curationSpecRepository, curationRepository, curationExtensionRepository);
  }

  @Test
  @DisplayName("새 큐레이션 스펙을 저장할 수 있다")
  void createSpec_신규_스펙_저장() {
    CurationSpec result =
        curationV2Service.createSpec(
            new CurationSpecCreateRequest(
                "RECOMMENDED_WHISKY",
                "추천 위스키",
                "개인화 추천",
                Map.of("type", "object", "required", List.of("source", "alcohol")),
                Map.of("type", "object"),
                "BOTTLE_NOTE",
                null));

    assertThat(result.getId()).isNotNull();
    assertThat(result.getVersion()).isEqualTo(1);
    assertThat(result.getIsActive()).isTrue();
  }

  @Test
  @DisplayName("중복 코드로 큐레이션 스펙을 저장하면 예외가 발생한다")
  void createSpec_중복_코드_예외() {
    curationV2Service.createSpec(
        new CurationSpecCreateRequest(
            "RECOMMENDED_WHISKY",
            "추천 위스키",
            null,
            Map.of("type", "object", "required", List.of("source", "alcohol")),
            Map.of("type", "object"),
            "BOTTLE_NOTE",
            1));

    assertThatThrownBy(
            () ->
                curationV2Service.createSpec(
                    new CurationSpecCreateRequest(
                        "RECOMMENDED_WHISKY", "중복 추천", null, Map.of(), Map.of(), "BOTTLE_NOTE", 1)))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", CurationExceptionCode.CURATION_SPEC_DUPLICATE_CODE);
  }

  @Test
  @DisplayName("큐레이션 저장 시 본문과 payload 확장을 함께 저장한다")
  void createCuration_본문과_payload_저장() {
    CurationSpec spec =
        curationV2Service.createSpec(
            new CurationSpecCreateRequest(
                "RECOMMENDED_WHISKY",
                "추천 위스키",
                null,
                Map.of("type", "object", "required", List.of("source", "alcohol")),
                Map.of("type", "object"),
                "BOTTLE_NOTE",
                1));

    Curation curation =
        curationV2Service.createCuration(
            new CurationCreateRequest(
                spec.getId(),
                "비 오는 날 위스키",
                "스모키 위스키 추천",
                List.of("https://cdn.example.com/cover.jpg"),
                LocalDate.of(2026, 5, 15),
                LocalDate.of(2026, 6, 15),
                10,
                true,
                Map.of("source", "BOTTLE_NOTE", "alcohol", Map.of("korName", "테스트"))));

    CurationExtension extension = curationV2Service.getExtension(curation.getId());

    assertThat(curation.getSpecId()).isEqualTo(spec.getId());
    assertThat(curation.getDisplayOrder()).isEqualTo(10);
    assertThat(extension.getSpecId()).isEqualTo(spec.getId());
    assertThat(new ObjectMapper().valueToTree(extension.getPayload()).path("source").asText())
        .isEqualTo("BOTTLE_NOTE");
  }

  @Test
  @DisplayName("존재하지 않는 스펙으로 큐레이션을 저장하면 예외가 발생한다")
  void createCuration_스펙_없음_예외() {
    assertThatThrownBy(
            () ->
                curationV2Service.createCuration(
                    new CurationCreateRequest(
                        999L,
                        "없는 스펙",
                        null,
                        List.of("https://cdn.example.com/cover.jpg"),
                        null,
                        null,
                        0,
                        true,
                        Map.of())))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", CurationExceptionCode.CURATION_SPEC_NOT_FOUND);
  }
}
