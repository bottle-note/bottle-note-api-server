package app.bottlenote.alcohols.service;

import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.repository.JpaRegionQueryRepository;
import app.bottlenote.shared.alcohols.dto.response.RegionsItem;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@DisplayName("[unit] [service] RegionService")
@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

  @InjectMocks private AlcoholReferenceService regionService;

  @Mock private JpaRegionQueryRepository regionQueryRepository;

  @Test
  @DisplayName("지역목록을 조회 할 수 있다.")
  void testFindAll() {
    // given
    List<RegionsItem> response =
        List.of(
            RegionsItem.of(1L, "스코틀랜드/로우랜드", "Scotland/Lowlands", "가벼운 맛이 특징인 로우랜드 위스키"),
            RegionsItem.of(
                2L,
                "스코틀랜드/하이랜드",
                "Scotland/Highlands",
                "맛의 다양성이 특징인 하이랜드 위스키, 해안의 짠맛부터 달콤하고 과일 맛까지"),
            RegionsItem.of(3L, "스코틀랜드/아일랜드", "Scotland/Ireland", "부드러운 맛이 특징인 아일랜드 위스키"),
            RegionsItem.of(11L, "프랑스", "France", "주로 브랜디와 와인 생산지로 유명하지만 위스키도 생산"),
            RegionsItem.of(12L, "스웨덴", "Sweden", "실험적인 방법으로 만드는 스웨덴 위스키"));

    // When
    when(regionQueryRepository.findAllRegionsResponse()).thenReturn(response);

    // Then
    List<RegionsItem> regions = regionService.findAllRegion();
    Assertions.assertEquals(response.size(), regions.size());
    Assertions.assertEquals(response.get(0).getRegionId(), regions.get(0).getRegionId());
  }
}
