package app.bottlenote.banner.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.dto.response.BannerResponse;
import app.bottlenote.banner.fixture.InMemoryBannerRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("BannerQueryService 단위 테스트")
class BannerQueryServiceTest {

  @Test
  @DisplayName("활성 배너를 조회할 때 동영상 대표 이미지 URL을 반환한다")
  void 활성_배너를_조회할_때_posterUrl을_반환한다() {
    InMemoryBannerRepository repository = new InMemoryBannerRepository();
    repository.save(
        Banner.builder()
            .name("동영상 배너")
            .imageUrl("https://example.com/banner.mp4")
            .posterUrl("https://example.com/poster.jpg")
            .bannerType(BannerType.CURATION)
            .sortOrder(1)
            .isActive(true)
            .build());
    BannerQueryService service = new BannerQueryService(repository);

    List<BannerResponse> result = service.getActiveBanners(1);

    assertThat(result)
        .singleElement()
        .extracting(BannerResponse::getPosterUrl)
        .isEqualTo("https://example.com/poster.jpg");
  }
}
