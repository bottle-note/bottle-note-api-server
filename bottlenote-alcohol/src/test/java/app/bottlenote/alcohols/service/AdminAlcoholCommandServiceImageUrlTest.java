package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.dto.request.AdminAlcoholUpsertRequest;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.fixture.InMemoryDistilleryRepository;
import app.bottlenote.alcohols.fixture.InMemoryRegionRepository;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

@Tag("unit")
@DisplayName("AdminAlcoholCommandService imageUrl optional 단위 테스트")
class AdminAlcoholCommandServiceImageUrlTest {

  private InMemoryAlcoholQueryRepository alcoholQueryRepository;
  private InMemoryRegionRepository regionRepository;
  private InMemoryDistilleryRepository distilleryRepository;
  private AdminAlcoholCommandService service;

  private Region region;
  private Distillery distillery;

  @BeforeEach
  void setUp() {
    alcoholQueryRepository = new InMemoryAlcoholQueryRepository();
    regionRepository = new InMemoryRegionRepository();
    distilleryRepository = new InMemoryDistilleryRepository();
    service =
        new AdminAlcoholCommandService(
            alcoholQueryRepository,
            regionRepository,
            distilleryRepository,
            List.of(),
            new InMemoryAlcoholsTastingTagsRepository(),
            new InMemoryTastingTagRepository(),
            event -> {});

    region =
        regionRepository.save(
            Region.builder().korName("스페이사이드").engName("Speyside").sortOrder(1).build());
    distillery =
        distilleryRepository.save(
            Distillery.builder().korName("글렌피딕").engName("Glenfiddich").sortOrder(1).build());
  }

  @Test
  @DisplayName("생성 시 imageUrl 누락을 허용하고 저장된 imageUrl은 null 이다")
  void create_whenImageUrlMissing_allowsNullImage() {
    AdminAlcoholUpsertRequest request =
        new AdminAlcoholUpsertRequest(
            "이미지 없는 위스키",
            "No Image Whisky",
            "40%",
            AlcoholType.WHISKY,
            "싱글 몰트",
            "Single Malt",
            AlcoholCategoryGroup.SINGLE_MALT,
            region.getId(),
            distillery.getId(),
            "12",
            "Oak",
            null,
            "설명",
            "700ml",
            null);

    var result = service.createAlcohol(request);

    Alcohol saved = alcoholQueryRepository.findById(result.targetId()).orElseThrow();
    assertThat(saved.getImageUrl()).isNull();
  }

  @Test
  @DisplayName("수정 시 imageUrl 누락이면 기존 이미지를 유지한다")
  void update_whenImageUrlMissing_keepsExistingImage() {
    RecordingEventPublisher events = new RecordingEventPublisher();
    service =
        new AdminAlcoholCommandService(
            alcoholQueryRepository,
            regionRepository,
            distilleryRepository,
            List.of(),
            new InMemoryAlcoholsTastingTagsRepository(),
            new InMemoryTastingTagRepository(),
            events);

    Alcohol existing =
        alcoholQueryRepository.save(
            Alcohol.builder()
                .korName("기존")
                .engName("Existing")
                .abv("40%")
                .type(AlcoholType.WHISKY)
                .korCategory("싱글 몰트")
                .engCategory("Single Malt")
                .categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
                .region(region)
                .distillery(distillery)
                .age("12")
                .cask("Oak")
                .imageUrl("https://cdn.example.com/keep.jpg")
                .description("desc")
                .volume("700ml")
                .build());

    AdminAlcoholUpsertRequest request =
        new AdminAlcoholUpsertRequest(
            "수정됨",
            "Updated",
            "43%",
            AlcoholType.WHISKY,
            "싱글 몰트",
            "Single Malt",
            AlcoholCategoryGroup.SINGLE_MALT,
            region.getId(),
            distillery.getId(),
            "18",
            "Sherry",
            null,
            "수정 설명",
            "750ml",
            null);

    service.updateAlcohol(existing.getId(), request);

    Alcohol updated = alcoholQueryRepository.findById(existing.getId()).orElseThrow();
    assertThat(updated.getImageUrl()).isEqualTo("https://cdn.example.com/keep.jpg");
    assertThat(updated.getKorName()).isEqualTo("수정됨");
    assertThat(events.events()).isEmpty();
  }

  private static final class RecordingEventPublisher implements ApplicationEventPublisher {
    private final java.util.List<Object> events = new java.util.ArrayList<>();

    @Override
    public void publishEvent(Object event) {
      events.add(event);
    }

    java.util.List<Object> events() {
      return events;
    }
  }
}
