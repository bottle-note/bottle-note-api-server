package app.bottlenote.banner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.exception.BannerException;
import app.bottlenote.banner.exception.BannerExceptionCode;
import app.bottlenote.banner.fixture.InMemoryBannerRepository;
import app.bottlenote.global.dto.request.AdminBulkReorderRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AdminBannerService 단위 테스트")
class AdminBannerServiceTest {

  private InMemoryBannerRepository bannerRepository;
  private AdminBannerService adminBannerService;

  @BeforeEach
  void setUp() {
    bannerRepository = new InMemoryBannerRepository();
    adminBannerService = new AdminBannerService(bannerRepository);
  }

  @Test
  @DisplayName("요청 ID가 주어졌을 때 전체 배너 목록의 맨 앞으로 재배치한다")
  void reorderToFront_whenIdsRequested_updatesRelativeOrder() {
    Banner first = saveBanner("기존 맨 앞", 1);
    Banner second = saveBanner("두 번째", 10);
    Banner third = saveBanner("세 번째", 20);
    Banner fourth = saveBanner("네 번째", 30);
    Banner fifth = saveBanner("다섯 번째", 40);

    adminBannerService.reorder(
        new AdminBulkReorderRequest(
            List.of(third.getId(), second.getId(), fifth.getId(), fourth.getId())));

    List<Banner> result = bannerRepository.findAllOrderBySortOrderAsc();
    assertThat(result)
        .extracting(Banner::getId)
        .containsExactly(
            third.getId(), second.getId(), fifth.getId(), fourth.getId(), first.getId());
    assertThat(result).extracting(Banner::getSortOrder).containsExactly(1, 10, 20, 30, 40);
  }

  @Test
  @DisplayName("요청 ID가 중복될 때 예외가 발생한다")
  void reorder_whenDuplicateIds_throwsException() {
    Banner banner = saveBanner("배너", 1);

    assertThatThrownBy(
            () ->
                adminBannerService.reorder(
                    new AdminBulkReorderRequest(List.of(banner.getId(), banner.getId()))))
        .isInstanceOf(BannerException.class)
        .extracting("exceptionCode")
        .isEqualTo(BannerExceptionCode.BANNER_REORDER_DUPLICATE_ID);
  }

  @Test
  @DisplayName("존재하지 않는 ID가 포함될 때 예외가 발생한다")
  void reorder_whenUnknownIdRequested_throwsException() {
    Banner banner = saveBanner("배너", 1);

    assertThatThrownBy(
            () ->
                adminBannerService.reorder(
                    new AdminBulkReorderRequest(List.of(banner.getId(), 999L))))
        .isInstanceOf(BannerException.class)
        .extracting("exceptionCode")
        .isEqualTo(BannerExceptionCode.BANNER_NOT_FOUND);
  }

  private Banner saveBanner(String name, int sortOrder) {
    return bannerRepository.save(
        Banner.builder()
            .name(name)
            .imageUrl("https://example.com/" + name + ".jpg")
            .bannerType(BannerType.CURATION)
            .sortOrder(sortOrder)
            .isActive(true)
            .build());
  }
}
