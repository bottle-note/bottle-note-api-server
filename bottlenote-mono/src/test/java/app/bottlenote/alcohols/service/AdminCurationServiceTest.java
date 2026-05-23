package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryCurationKeywordRepository;
import app.bottlenote.global.dto.request.AdminBulkReorderRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AdminCurationService 단위 테스트")
class AdminCurationServiceTest {

  private InMemoryCurationKeywordRepository curationKeywordRepository;
  private AdminCurationService adminCurationService;

  @BeforeEach
  void setUp() {
    curationKeywordRepository = new InMemoryCurationKeywordRepository();
    adminCurationService =
        new AdminCurationService(curationKeywordRepository, new InMemoryAlcoholQueryRepository());
  }

  @Test
  @DisplayName("요청 ID가 주어졌을 때 전체 큐레이션 목록의 맨 앞으로 재배치한다")
  void reorderToFront_whenIdsRequested_updatesRelativeOrder() {
    CurationKeyword first = saveCuration("기존 맨 앞", 1);
    CurationKeyword second = saveCuration("두 번째", 10);
    CurationKeyword third = saveCuration("세 번째", 20);
    CurationKeyword fourth = saveCuration("네 번째", 30);
    CurationKeyword fifth = saveCuration("다섯 번째", 40);

    adminCurationService.reorder(
        new AdminBulkReorderRequest(
            List.of(third.getId(), second.getId(), fifth.getId(), fourth.getId())));

    List<CurationKeyword> result = curationKeywordRepository.findAllOrderByDisplayOrderAsc();
    assertThat(result)
        .extracting(CurationKeyword::getId)
        .containsExactly(
            third.getId(), second.getId(), fifth.getId(), fourth.getId(), first.getId());
    assertThat(result)
        .extracting(CurationKeyword::getDisplayOrder)
        .containsExactly(1, 10, 20, 30, 40);
  }

  @Test
  @DisplayName("요청 ID가 중복될 때 예외가 발생한다")
  void reorder_whenDuplicateIds_throwsException() {
    CurationKeyword curation = saveCuration("큐레이션", 1);

    assertThatThrownBy(
            () ->
                adminCurationService.reorder(
                    new AdminBulkReorderRequest(List.of(curation.getId(), curation.getId()))))
        .isInstanceOf(AlcoholException.class)
        .extracting("exceptionCode")
        .isEqualTo(AlcoholExceptionCode.CURATION_REORDER_DUPLICATE_ID);
  }

  @Test
  @DisplayName("존재하지 않는 ID가 포함될 때 예외가 발생한다")
  void reorder_whenUnknownIdRequested_throwsException() {
    CurationKeyword curation = saveCuration("큐레이션", 1);

    assertThatThrownBy(
            () ->
                adminCurationService.reorder(
                    new AdminBulkReorderRequest(List.of(curation.getId(), 999L))))
        .isInstanceOf(AlcoholException.class)
        .extracting("exceptionCode")
        .isEqualTo(AlcoholExceptionCode.CURATION_NOT_FOUND);
  }

  private CurationKeyword saveCuration(String name, int displayOrder) {
    return curationKeywordRepository.save(
        CurationKeyword.create(
            name, name + " 설명", "https://example.com/" + name + ".jpg", displayOrder, Set.of()));
  }
}
