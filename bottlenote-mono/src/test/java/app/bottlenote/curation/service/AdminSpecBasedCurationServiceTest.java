package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.dto.request.CurationCreateRequest;
import app.bottlenote.curation.dto.request.CurationSearchRequest;
import app.bottlenote.curation.dto.request.CurationSpecCreateRequest;
import app.bottlenote.curation.dto.request.CurationUpdateRequest;
import app.bottlenote.curation.dto.response.AdminSpecBasedCurationDetailResponse;
import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.exception.CurationExceptionCode;
import app.bottlenote.curation.fixture.InMemoryCurationExtensionRepository;
import app.bottlenote.curation.fixture.InMemoryCurationRepository;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import app.bottlenote.global.data.response.GlobalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AdminSpecBasedCurationService 단위 테스트")
class AdminSpecBasedCurationServiceTest {

  InMemoryCurationSpecRepository curationSpecRepository;
  InMemoryCurationRepository curationRepository;
  InMemoryCurationExtensionRepository curationExtensionRepository;
  CurationV2Service curationV2Service;
  AdminSpecBasedCurationService adminSpecBasedCurationService;

  @BeforeEach
  void setUp() {
    curationSpecRepository = new InMemoryCurationSpecRepository();
    curationRepository = new InMemoryCurationRepository();
    curationExtensionRepository = new InMemoryCurationExtensionRepository();
    ObjectMapper objectMapper = new ObjectMapper();
    curationV2Service =
        new CurationV2Service(
            curationSpecRepository, curationRepository, curationExtensionRepository);
    adminSpecBasedCurationService =
        new AdminSpecBasedCurationService(
            curationSpecRepository,
            curationRepository,
            curationExtensionRepository,
            new CurationPayloadValidator(objectMapper));
  }

  @Test
  @DisplayName("큐레이션을 생성하면 imageUrls 첫 번째 이미지를 coverImageUrl에 저장하고 payload를 함께 저장한다")
  void create_이미지와_payload_저장() {
    CurationSpec spec = createSpec();

    var result = adminSpecBasedCurationService.create(createRequest(spec.getId()));

    AdminSpecBasedCurationDetailResponse detail =
        adminSpecBasedCurationService.getDetail(result.targetId());
    assertThat(result.code()).isEqualTo("CURATION_CREATED");
    assertThat(detail.coverImageUrl()).isEqualTo("https://cdn.example.com/cover.jpg");
    assertThat(detail.imageUrls())
        .containsExactly("https://cdn.example.com/cover.jpg", "https://cdn.example.com/second.jpg");
    assertThat(detail.spec().code()).isEqualTo("RECOMMENDED_WHISKY");
    assertThat(new ObjectMapper().valueToTree(detail.payload()).path("source").asText())
        .isEqualTo("BOTTLE_NOTE");
  }

  @Test
  @DisplayName("큐레이션 스펙 상세를 조회할 수 있다")
  void getSpecDetail_스펙_상세_조회() {
    CurationSpec spec = createSpec();

    var result = adminSpecBasedCurationService.getSpecDetail(spec.getId());

    assertThat(result.id()).isEqualTo(spec.getId());
    assertThat(result.code()).isEqualTo("RECOMMENDED_WHISKY");
    assertThat(result.requestSpec()).containsKey("required");
  }

  @Test
  @DisplayName("payload가 requestSpec required 필드를 만족하지 않으면 예외가 발생한다")
  void create_payload_검증_실패() {
    CurationSpec spec = createSpec();

    assertThatThrownBy(
            () ->
                adminSpecBasedCurationService.create(
                    new CurationCreateRequest(
                        spec.getId(),
                        "잘못된 큐레이션",
                        null,
                        List.of("https://cdn.example.com/cover.jpg"),
                        null,
                        null,
                        0,
                        true,
                        Map.of("source", "BOTTLE_NOTE"))))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", CurationExceptionCode.CURATION_PAYLOAD_INVALID);
  }

  @Test
  @DisplayName("큐레이션을 수정하면 본문과 extension payload를 함께 갱신한다")
  void update_본문과_payload_수정() {
    CurationSpec spec = createSpec();
    Long curationId = adminSpecBasedCurationService.create(createRequest(spec.getId())).targetId();

    var result =
        adminSpecBasedCurationService.update(
            curationId,
            new CurationUpdateRequest(
                spec.getId(),
                "수정된 큐레이션",
                "수정된 설명",
                List.of("https://cdn.example.com/updated.jpg"),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                5,
                false,
                Map.of("source", "MANUAL", "alcohol", Map.of("korName", "수동 입력"))));

    AdminSpecBasedCurationDetailResponse detail =
        adminSpecBasedCurationService.getDetail(curationId);
    assertThat(result.code()).isEqualTo("CURATION_UPDATED");
    assertThat(detail.name()).isEqualTo("수정된 큐레이션");
    assertThat(detail.isActive()).isFalse();
    assertThat(detail.imageUrls()).containsExactly("https://cdn.example.com/updated.jpg");
  }

  @Test
  @DisplayName("큐레이션 목록을 조회할 수 있다")
  void search_목록_조회() {
    CurationSpec spec = createSpec();
    adminSpecBasedCurationService.create(createRequest(spec.getId()));

    GlobalResponse result =
        adminSpecBasedCurationService.search(new CurationSearchRequest("", null, 0, 20));

    assertThat(result.getData()).asList().hasSize(1);
  }

  private CurationSpec createSpec() {
    return curationV2Service.createSpec(
        new CurationSpecCreateRequest(
            "RECOMMENDED_WHISKY",
            "추천 위스키",
            null,
            Map.of("type", "object", "required", List.of("source", "alcohol")),
            Map.of("type", "object"),
            "alcohol",
            1));
  }

  private CurationCreateRequest createRequest(Long specId) {
    return new CurationCreateRequest(
        specId,
        "비 오는 날 위스키",
        "스모키 위스키 추천",
        List.of("https://cdn.example.com/cover.jpg", "https://cdn.example.com/second.jpg"),
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 30),
        1,
        true,
        Map.of("source", "BOTTLE_NOTE", "alcohol", Map.of("korName", "테스트 위스키")));
  }
}
