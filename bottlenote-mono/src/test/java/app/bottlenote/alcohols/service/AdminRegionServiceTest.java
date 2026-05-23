package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.dto.request.AdminRegionCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminRegionSortOrderRequest;
import app.bottlenote.alcohols.dto.request.AdminRegionUpdateRequest;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.fixture.InMemoryRegionRepository;
import app.bottlenote.global.dto.request.AdminBulkReorderRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AdminRegionService 단위 테스트")
class AdminRegionServiceTest {

  private InMemoryRegionRepository regionRepository;
  private AdminRegionService service;

  @BeforeEach
  void setUp() {
    regionRepository = new InMemoryRegionRepository();
    service = new AdminRegionService(regionRepository);
  }

  private Region saveRegion(String korName, String engName, Region parent, int sortOrder) {
    Region region =
        Region.builder()
            .korName(korName)
            .engName(engName)
            .continent(null)
            .description(null)
            .sortOrder(sortOrder)
            .parent(parent)
            .build();
    return regionRepository.save(region);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("정상 케이스에서 지역을 생성할 수 있다")
    void create_whenValid_savesRegion() {
      AdminRegionCreateRequest request =
          AdminRegionCreateRequest.builder()
              .korName("스코틀랜드")
              .engName("Scotland")
              .continent("Europe")
              .description("스카치")
              .sortOrder(10)
              .build();

      var result = service.create(request);

      assertThat(result.code()).isEqualTo("REGION_CREATED");
      assertThat(result.targetId()).isNotNull();
    }

    @Test
    @DisplayName("한글 이름이 중복되면 REGION_DUPLICATE_KOR_NAME 예외가 발생한다")
    void create_whenKorNameDuplicated_throws() {
      saveRegion("스코틀랜드", "Scotland", null, 10);

      AdminRegionCreateRequest request =
          AdminRegionCreateRequest.builder().korName("스코틀랜드").engName("ScotlandUk").build();

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_DUPLICATE_KOR_NAME);
    }

    @Test
    @DisplayName("영문 이름이 중복되면 REGION_DUPLICATE_ENG_NAME 예외가 발생한다")
    void create_whenEngNameDuplicated_throws() {
      saveRegion("스코틀랜드", "Scotland", null, 10);

      AdminRegionCreateRequest request =
          AdminRegionCreateRequest.builder().korName("스코틀랜드영국").engName("Scotland").build();

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_DUPLICATE_ENG_NAME);
    }

    @Test
    @DisplayName("부모 지역이 존재하지 않으면 REGION_PARENT_NOT_FOUND 예외가 발생한다")
    void create_whenParentNotFound_throws() {
      AdminRegionCreateRequest request =
          AdminRegionCreateRequest.builder().korName("아일라").engName("Islay").parentId(999L).build();

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_PARENT_NOT_FOUND);
    }

    @Test
    @DisplayName("부모가 이미 자식 지역(2단계)일 때 REGION_MAX_DEPTH_EXCEEDED 예외가 발생한다")
    void create_whenParentIsChild_throws() {
      Region root = saveRegion("스코틀랜드", "Scotland", null, 10);
      Region child = saveRegion("하이랜드", "Highland", root, 20);

      AdminRegionCreateRequest request =
          AdminRegionCreateRequest.builder()
              .korName("아일라")
              .engName("Islay")
              .parentId(child.getId())
              .build();

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_MAX_DEPTH_EXCEEDED);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("정상 케이스에서 지역을 수정한다")
    void update_whenValid_modifiesRegion() {
      Region region = saveRegion("스코트랜드", "ScotlandTypo", null, 10);

      AdminRegionUpdateRequest request =
          AdminRegionUpdateRequest.builder()
              .korName("스코틀랜드")
              .engName("Scotland")
              .continent("Europe")
              .description("정정")
              .sortOrder(10)
              .build();

      var result = service.update(region.getId(), request);

      assertThat(result.code()).isEqualTo("REGION_UPDATED");
      assertThat(regionRepository.findById(region.getId()).orElseThrow().getKorName())
          .isEqualTo("스코틀랜드");
    }

    @Test
    @DisplayName("자기 자신을 부모로 지정하면 REGION_PARENT_CYCLE 예외가 발생한다")
    void update_whenParentIsSelf_throws() {
      Region region = saveRegion("스코틀랜드", "Scotland", null, 10);

      AdminRegionUpdateRequest request =
          AdminRegionUpdateRequest.builder()
              .korName("스코틀랜드")
              .engName("Scotland")
              .parentId(region.getId())
              .sortOrder(10)
              .build();

      assertThatThrownBy(() -> service.update(region.getId(), request))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_PARENT_CYCLE);
    }

    @Test
    @DisplayName("자식이 있는 지역을 다른 지역의 자식으로 변경하면 REGION_MAX_DEPTH_EXCEEDED 예외가 발생한다")
    void update_whenSelfHasChildren_throws() {
      Region root = saveRegion("스코틀랜드", "Scotland", null, 10);
      Region another = saveRegion("아일랜드", "Ireland", null, 20);
      saveRegion("하이랜드", "Highland", root, 30);

      AdminRegionUpdateRequest request =
          AdminRegionUpdateRequest.builder()
              .korName("스코틀랜드")
              .engName("Scotland")
              .parentId(another.getId())
              .sortOrder(10)
              .build();

      assertThatThrownBy(() -> service.update(root.getId(), request))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_MAX_DEPTH_EXCEEDED);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("자식과 위스키가 없으면 삭제할 수 있다")
    void delete_whenNoChildAndNoAlcohol_removesRegion() {
      Region region = saveRegion("스코틀랜드", "Scotland", null, 10);

      var result = service.delete(region.getId());

      assertThat(result.code()).isEqualTo("REGION_DELETED");
      assertThat(regionRepository.findById(region.getId())).isEmpty();
    }

    @Test
    @DisplayName("자식 지역이 존재하면 REGION_HAS_CHILDREN 예외가 발생한다")
    void delete_whenHasChildren_throws() {
      Region root = saveRegion("스코틀랜드", "Scotland", null, 10);
      saveRegion("하이랜드", "Highland", root, 20);

      assertThatThrownBy(() -> service.delete(root.getId()))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_HAS_CHILDREN);
    }

    @Test
    @DisplayName("연결된 위스키가 존재하면 REGION_HAS_ALCOHOLS 예외가 발생한다")
    void delete_whenHasAlcohols_throws() {
      Region region = saveRegion("스코틀랜드", "Scotland", null, 10);
      regionRepository.setAlcoholCount(region.getId(), 5L);

      assertThatThrownBy(() -> service.delete(region.getId()))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_HAS_ALCOHOLS);
    }
  }

  @Nested
  @DisplayName("updateSortOrder")
  class UpdateSortOrder {

    @Test
    @DisplayName("정렬 변경 시 같은 sortOrder 이상인 다른 지역을 +1 밀어낸다")
    void updateSortOrder_reordersOtherRegions() {
      Region a = saveRegion("A", "A", null, 10);
      Region b = saveRegion("B", "B", null, 20);
      Region target = saveRegion("T", "T", null, 100);

      service.updateSortOrder(target.getId(), new AdminRegionSortOrderRequest(20));

      assertThat(regionRepository.findById(target.getId()).orElseThrow().getSortOrder())
          .isEqualTo(20);
      assertThat(regionRepository.findById(b.getId()).orElseThrow().getSortOrder()).isEqualTo(21);
      assertThat(regionRepository.findById(a.getId()).orElseThrow().getSortOrder()).isEqualTo(10);
    }

    @Test
    @DisplayName("동일한 sortOrder로 변경하면 변경 없음(no-op)이다")
    void updateSortOrder_whenSameValue_noChange() {
      Region target = saveRegion("T", "T", null, 50);

      service.updateSortOrder(target.getId(), new AdminRegionSortOrderRequest(50));

      assertThat(regionRepository.findById(target.getId()).orElseThrow().getSortOrder())
          .isEqualTo(50);
    }
  }

  @Nested
  @DisplayName("bulk reorder")
  class BulkReorder {

    @Test
    @DisplayName("전체 지역 bulk reorder는 요청 ID를 전체 지역 목록의 맨 앞으로 재배치한다")
    void reorderToFront_whenIdsRequested_updatesRelativeOrder() {
      Region first = saveRegion("기존 맨 앞", "First", null, 1);
      Region second = saveRegion("두 번째", "Second", null, 10);
      Region third = saveRegion("세 번째", "Third", null, 20);
      Region fourth = saveRegion("네 번째", "Fourth", null, 30);
      Region fifth = saveRegion("다섯 번째", "Fifth", null, 40);

      service.reorder(
          new AdminBulkReorderRequest(
              List.of(third.getId(), second.getId(), fifth.getId(), fourth.getId())));

      List<Region> result = regionRepository.findAllOrderBySortOrderAsc();
      assertThat(result)
          .extracting(Region::getId)
          .containsExactly(
              third.getId(), second.getId(), fifth.getId(), fourth.getId(), first.getId());
      assertThat(result).extracting(Region::getSortOrder).containsExactly(1, 10, 20, 30, 40);
    }

    @Test
    @DisplayName("자식 지역 bulk reorder는 같은 parentId의 직접 자식만 재배치한다")
    void reorderChildrenToFront_whenIdsRequested_updatesOnlyChildrenOfParent() {
      Region parent = saveRegion("스코틀랜드", "Scotland", null, 1);
      Region anotherParent = saveRegion("아일랜드", "Ireland", null, 2);
      Region first = saveRegion("기존 맨 앞", "First", parent, 1);
      Region second = saveRegion("두 번째", "Second", parent, 10);
      Region third = saveRegion("세 번째", "Third", parent, 20);
      Region fourth = saveRegion("네 번째", "Fourth", parent, 30);
      Region otherChild = saveRegion("다른 부모 자식", "OtherChild", anotherParent, 5);

      service.reorderChildren(
          parent.getId(), new AdminBulkReorderRequest(List.of(third.getId(), second.getId())));

      List<Region> result = regionRepository.findAllByParentIdOrderBySortOrderAsc(parent.getId());
      assertThat(result)
          .extracting(Region::getId)
          .containsExactly(third.getId(), second.getId(), first.getId(), fourth.getId());
      assertThat(result).extracting(Region::getSortOrder).containsExactly(1, 10, 20, 30);
      assertThat(regionRepository.findById(otherChild.getId()).orElseThrow().getSortOrder())
          .isEqualTo(5);
    }

    @Test
    @DisplayName("자식 지역 bulk reorder에 다른 부모의 ID가 포함되면 예외가 발생한다")
    void reorderChildren_whenOtherParentChildIncluded_throwsException() {
      Region parent = saveRegion("스코틀랜드", "Scotland", null, 1);
      Region anotherParent = saveRegion("아일랜드", "Ireland", null, 2);
      Region child = saveRegion("하이랜드", "Highland", parent, 10);
      Region otherChild = saveRegion("더블린", "Dublin", anotherParent, 20);

      assertThatThrownBy(
              () ->
                  service.reorderChildren(
                      parent.getId(),
                      new AdminBulkReorderRequest(List.of(child.getId(), otherChild.getId()))))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_REORDER_SCOPE_MISMATCH);
    }
  }

  @Nested
  @DisplayName("getDetail")
  class GetDetail {

    @Test
    @DisplayName("지역과 부모, 자식 여부, 위스키 카운트를 함께 반환한다")
    void getDetail_returnsAggregatedInfo() {
      Region root = saveRegion("스코틀랜드", "Scotland", null, 10);
      Region child = saveRegion("하이랜드", "Highland", root, 20);
      regionRepository.setAlcoholCount(child.getId(), 7L);

      var detail = service.getDetail(child.getId());

      assertThat(detail.id()).isEqualTo(child.getId());
      assertThat(detail.parentId()).isEqualTo(root.getId());
      assertThat(detail.parentKorName()).isEqualTo("스코틀랜드");
      assertThat(detail.hasChildren()).isFalse();
      assertThat(detail.alcoholCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("존재하지 않는 지역 조회 시 REGION_NOT_FOUND 예외가 발생한다")
    void getDetail_whenNotFound_throws() {
      assertThatThrownBy(() -> service.getDetail(999L))
          .isInstanceOf(AlcoholException.class)
          .extracting("exceptionCode")
          .isEqualTo(AlcoholExceptionCode.REGION_NOT_FOUND);
    }
  }
}
