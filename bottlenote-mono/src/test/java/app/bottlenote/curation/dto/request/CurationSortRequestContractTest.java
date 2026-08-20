package app.bottlenote.curation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.constant.CurationSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("큐레이션 선택 정렬 요청 계약")
class CurationSortRequestContractTest {

  @Test
  @DisplayName("Admin과 Product 요청은 누락된 정렬을 노출 시작일 내림차순으로 기본화한다")
  void requests_defaultToExposureStartDateDescending() {
    CurationSearchRequest admin = new CurationSearchRequest(null, null, null, null, null, null, null);
    CurationFeedSearchRequest product =
        new CurationFeedSearchRequest(List.of("RECOMMENDED_WHISKY"), null, null, null, null, null);

    assertThat(admin.sortType()).isEqualTo(CurationSortType.EXPOSURE_START_DATE);
    assertThat(admin.sortOrder()).isEqualTo(SortOrder.DESC);
    assertThat(product.sortType()).isEqualTo(CurationSortType.EXPOSURE_START_DATE);
    assertThat(product.sortOrder()).isEqualTo(SortOrder.DESC);
  }

  @Test
  @DisplayName("큐레이션 정렬 속성은 노출 시작일과 수동 노출 순서만 제공한다")
  void sortType_exposesOnlyApprovedValues() {
    assertThat(CurationSortType.values())
        .containsExactly(CurationSortType.EXPOSURE_START_DATE, CurationSortType.DISPLAY_ORDER);
  }
}
