package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.SINGLE_MALT;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.dto.request.AdminAlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] AdminAlcoholLookupService page/size 목록")
class AdminAlcoholLookupServiceTest {

  private InMemoryAlcoholLookupSnapshotStore snapshotStore;
  private AdminAlcoholLookupService adminAlcoholLookupService;

  @BeforeEach
  void setUp() {
    InMemoryAlcoholQueryRepository alcoholQueryRepository = new InMemoryAlcoholQueryRepository();
    snapshotStore = new InMemoryAlcoholLookupSnapshotStore();
    adminAlcoholLookupService =
        new AdminAlcoholLookupService(
            new AlcoholLookupSnapshotService(alcoholQueryRepository, snapshotStore));
  }

  @Test
  @DisplayName("page와 size로 룩업 목록을 자른다")
  void lookup_whenPageAndSize_returnsOffsetSlice() {
    snapshotStore.replaceAll(createLookupSnapshotItems(5));

    var response =
        adminAlcoholLookupService.lookup(
            AdminAlcoholLookupRequest.builder().keyword("macallan").page(1).size(2).build());

    @SuppressWarnings("unchecked")
    List<AlcoholLookupItem> content = (List<AlcoholLookupItem>) response.getData();
    assertThat(content).extracting(AlcoholLookupItem::alcoholId).containsExactly(3L, 4L);
    assertThat(response.getMeta())
        .containsEntry("page", 1)
        .containsEntry("size", 2)
        .containsEntry("totalElements", 5L);
  }

  @Test
  @DisplayName("Product lookup 요청 타입 없이 필터를 적용한다")
  void lookup_whenKeywordFilter_returnsMatchingItems() {
    snapshotStore.replaceAll(
        List.of(
            lookupSnapshotItem(1L, "맥캘란 12년", "Macallan 12", "맥캘란", "Macallan"),
            lookupSnapshotItem(2L, "글렌피딕 12년", "Glenfiddich 12", "글렌피딕", "Glenfiddich")));

    var response =
        adminAlcoholLookupService.lookup(
            AdminAlcoholLookupRequest.builder().keyword("macallan").build());

    @SuppressWarnings("unchecked")
    List<AlcoholLookupItem> content = (List<AlcoholLookupItem>) response.getData();
    assertThat(content).extracting(AlcoholLookupItem::alcoholId).containsExactly(1L);
  }

  private List<AlcoholLookupSnapshotItem> createLookupSnapshotItems(int size) {
    return LongStream.rangeClosed(1, size)
        .mapToObj(id -> lookupSnapshotItem(id, "맥캘란 " + id, "Macallan " + id, "맥캘란", "Macallan"))
        .toList();
  }

  private AlcoholLookupSnapshotItem lookupSnapshotItem(
      Long alcoholId, String korName, String engName, String korDistillery, String engDistillery) {
    return AlcoholLookupSnapshotItem.from(
        new AlcoholLookupItem(
            alcoholId,
            korName,
            engName,
            "싱글몰트",
            "Single Malt",
            SINGLE_MALT,
            1L,
            "스페이사이드",
            "Speyside",
            10L,
            korDistillery,
            engDistillery,
            "https://example.com/alcohol.png"));
  }
}
