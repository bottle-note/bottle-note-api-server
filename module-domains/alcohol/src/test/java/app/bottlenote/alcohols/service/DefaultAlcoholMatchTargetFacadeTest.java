package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryDistilleryRepository;
import app.bottlenote.alcohols.fixture.InMemoryRegionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] DefaultAlcoholMatchTargetFacade")
class DefaultAlcoholMatchTargetFacadeTest {

  private final InMemoryDistilleryRepository distilleryRepository =
      new InMemoryDistilleryRepository();
  private final InMemoryRegionRepository regionRepository = new InMemoryRegionRepository();

  private DefaultAlcoholMatchTargetFacade facade;

  @BeforeEach
  void setUp() {
    facade =
        new DefaultAlcoholMatchTargetFacade(
            new InMemoryAlcoholQueryRepository(), distilleryRepository, regionRepository);

    distilleryRepository.save(
        Distillery.builder().korName("맥캘란 증류소").engName("Macallan").sortOrder(2).build());
    distilleryRepository.save(
        Distillery.builder().korName("글렌피딕 증류소").engName("Glenfiddich").sortOrder(1).build());

    regionRepository.save(
        Region.builder().korName("스페이사이드").engName("Speyside").sortOrder(2).build());
    regionRepository.save(Region.builder().korName("아일라").engName("Islay").sortOrder(1).build());
  }

  @Test
  @DisplayName("증류소 ID 목록을 넘기면 해당 증류소만 정렬 순서대로 조회한다")
  void 증류소를_id_목록으로_조회할_수_있다() {
    List<DistilleryMatchTargetItem> result = facade.findDistilleryTargetsByIds(List.of(1L, 2L));

    assertThat(result)
        .extracting(DistilleryMatchTargetItem::engName)
        .containsExactly("Glenfiddich", "Macallan");
  }

  @Test
  @DisplayName("존재하지 않는 증류소 ID만 넘기면 빈 목록을 반환한다")
  void 없는_증류소_id는_결과에서_제외된다() {
    assertThat(facade.findDistilleryTargetsByIds(List.of(999L))).isEmpty();
  }

  @Test
  @DisplayName("증류소 ID 목록이 비어 있으면 조회 없이 빈 목록을 반환한다")
  void 빈_증류소_id_목록은_빈_결과를_반환한다() {
    assertThat(facade.findDistilleryTargetsByIds(List.of())).isEmpty();
    assertThat(facade.findDistilleryTargetsByIds(null)).isEmpty();
  }

  @Test
  @DisplayName("지역 ID 목록을 넘기면 해당 지역만 정렬 순서대로 조회한다")
  void 지역을_id_목록으로_조회할_수_있다() {
    List<RegionMatchTargetItem> result = facade.findRegionTargetsByIds(List.of(1L, 2L));

    assertThat(result)
        .extracting(RegionMatchTargetItem::engName)
        .containsExactly("Islay", "Speyside");
  }

  @Test
  @DisplayName("지역 ID 목록이 비어 있으면 조회 없이 빈 목록을 반환한다")
  void 빈_지역_id_목록은_빈_결과를_반환한다() {
    assertThat(facade.findRegionTargetsByIds(List.of())).isEmpty();
    assertThat(facade.findRegionTargetsByIds(null)).isEmpty();
  }
}
