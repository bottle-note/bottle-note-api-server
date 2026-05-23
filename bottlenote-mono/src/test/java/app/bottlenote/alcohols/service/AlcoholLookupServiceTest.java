package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.SINGLE_MALT;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.dto.request.AlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("AlcoholLookupService 단위 테스트")
class AlcoholLookupServiceTest {
  private InMemoryAlcoholQueryRepository alcoholQueryRepository;
  private InMemoryAlcoholLookupSnapshotStore snapshotStore;
  private AlcoholLookupService alcoholLookupService;

  @BeforeEach
  void setUp() {
    alcoholQueryRepository = new InMemoryAlcoholQueryRepository();
    snapshotStore = new InMemoryAlcoholLookupSnapshotStore();
    alcoholLookupService = new AlcoholLookupService(alcoholQueryRepository, snapshotStore);
  }

  @Test
  @DisplayName("20,000건 lookup snapshot에서 검색 조건을 적용해 cursor 페이지를 반환한다")
  void lookup_whenSnapshotHas20000Items_returnsFilteredCursorPage() {
    // given
    snapshotStore.replaceAll(createLookupItems(20_000));
    AlcoholLookupRequest request =
        AlcoholLookupRequest.builder()
            .keyword("macallan speyside")
            .category("SINGLE_MALT")
            .regionId(1L)
            .distilleryId(10L)
            .cursor(20L)
            .pageSize(20L)
            .build();

    // when
    CursorResponse<AlcoholLookupItem> response = alcoholLookupService.lookup(request);

    // then
    assertThat(response.items()).hasSize(20);
    assertThat(response.items().get(0).alcoholId()).isEqualTo(21L);
    assertThat(response.pageable().getCurrentCursor()).isEqualTo(20L);
    assertThat(response.pageable().getCursor()).isEqualTo(40L);
    assertThat(response.pageable().getHasNext()).isTrue();
  }

  @Test
  @DisplayName("다중 키워드는 모든 단어가 포함된 술만 반환한다")
  void lookup_whenMultipleKeywords_returnsItemsContainingEveryKeyword() {
    // given
    snapshotStore.replaceAll(
        List.of(
            lookupItem(1L, "맥캘란 12년", "Macallan 12", "스페이사이드", "Speyside", "맥캘란", "Macallan"),
            lookupItem(
                2L, "글렌피딕 12년", "Glenfiddich 12", "스페이사이드", "Speyside", "글렌피딕", "Glenfiddich")));
    AlcoholLookupRequest request =
        AlcoholLookupRequest.builder().keyword("macallan speyside").pageSize(20L).build();

    // when
    CursorResponse<AlcoholLookupItem> response = alcoholLookupService.lookup(request);

    // then
    assertThat(response.items()).extracting(AlcoholLookupItem::alcoholId).containsExactly(1L);
  }

  @Test
  @DisplayName("Redis snapshot이 비어 있으면 DB fallback으로 조회한다")
  void lookup_whenSnapshotIsEmpty_usesDatabaseFallback() {
    // given
    alcoholQueryRepository.save(createAlcohol(1L));
    AlcoholLookupRequest request =
        AlcoholLookupRequest.builder().keyword("macallan").pageSize(20L).build();

    // when
    CursorResponse<AlcoholLookupItem> response = alcoholLookupService.lookup(request);

    // then
    assertThat(response.items()).extracting(AlcoholLookupItem::alcoholId).containsExactly(1L);
  }

  @Test
  @DisplayName("DB 원천 데이터를 Redis snapshot으로 동기화한다")
  void syncSnapshot_savesDatabaseLookupItemsToSnapshotStore() {
    // given
    alcoholQueryRepository.save(createAlcohol(1L));

    // when
    int syncedCount = alcoholLookupService.syncSnapshot();

    // then
    assertThat(syncedCount).isEqualTo(1);
    assertThat(snapshotStore.findAll())
        .extracting(AlcoholLookupItem::alcoholId)
        .containsExactly(1L);
  }

  private List<AlcoholLookupItem> createLookupItems(int size) {
    return LongStream.rangeClosed(1, size)
        .mapToObj(
            id ->
                lookupItem(
                    id, "맥캘란 " + id, "Macallan " + id, "스페이사이드", "Speyside", "맥캘란", "Macallan"))
        .toList();
  }

  private AlcoholLookupItem lookupItem(
      Long alcoholId,
      String korName,
      String engName,
      String korRegion,
      String engRegion,
      String korDistillery,
      String engDistillery) {
    return new AlcoholLookupItem(
        alcoholId,
        korName,
        engName,
        "싱글몰트",
        "Single Malt",
        SINGLE_MALT,
        1L,
        korRegion,
        engRegion,
        10L,
        korDistillery,
        engDistillery,
        "https://example.com/alcohol.png");
  }

  private Alcohol createAlcohol(Long alcoholId) {
    Region region = Region.builder().korName("스페이사이드").engName("Speyside").build();
    ReflectionTestUtils.setField(region, "id", 1L);

    Distillery distillery = Distillery.builder().korName("맥캘란").engName("Macallan").build();
    ReflectionTestUtils.setField(distillery, "id", 10L);

    Alcohol alcohol =
        Alcohol.builder()
            .korName("맥캘란 " + alcoholId)
            .engName("Macallan " + alcoholId)
            .korCategory("싱글몰트")
            .engCategory("Single Malt")
            .categoryGroup(SINGLE_MALT)
            .type(app.bottlenote.alcohols.constant.AlcoholType.WHISKY)
            .region(region)
            .distillery(distillery)
            .imageUrl("https://example.com/alcohol.png")
            .build();
    ReflectionTestUtils.setField(alcohol, "id", alcoholId);
    return alcohol;
  }
}
