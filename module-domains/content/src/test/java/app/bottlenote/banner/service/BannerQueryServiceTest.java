package app.bottlenote.banner.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.dto.response.BannerResponse;
import app.bottlenote.banner.fixture.InMemoryBannerRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("BannerQueryService 단위 테스트")
class BannerQueryServiceTest {

  private static final ZoneId ZONE = ZoneOffset.UTC;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 12, 0, 0);
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);

  private InMemoryBannerRepository repository;
  private BannerQueryService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryBannerRepository();
    service = new BannerQueryService(repository, FIXED_CLOCK);
  }

  @Test
  @DisplayName("활성 배너를 조회할 때 동영상 대표 이미지 URL을 반환한다")
  void 활성_배너를_조회할_때_posterUrl을_반환한다() {
    repository.save(
        Banner.builder()
            .name("동영상 배너")
            .imageUrl("https://example.com/banner.mp4")
            .posterUrl("https://example.com/poster.jpg")
            .bannerType(BannerType.CURATION)
            .sortOrder(1)
            .isActive(true)
            .build());

    List<BannerResponse> result = service.getActiveBanners(1);

    assertThat(result)
        .singleElement()
        .extracting(BannerResponse::getPosterUrl)
        .isEqualTo("https://example.com/poster.jpg");
  }

  @Test
  @DisplayName("활성 배너가 노출 기간 안에 있을 때 조회 결과에 포함한다")
  void 노출_기간_안의_활성_배너를_조회할_수_있다() {
    // given
    repository.save(banner("기간 내", 1, true, NOW.minusDays(1), NOW.plusDays(1)));

    // when
    List<BannerResponse> result = service.getActiveBanners(10);

    // then
    assertThat(result).extracting(BannerResponse::getName).containsExactly("기간 내");
  }

  @Test
  @DisplayName("비활성 배너일 때 노출 기간 안이어도 조회 결과에서 제외한다")
  void 비활성_배너를_제외할_수_있다() {
    // given
    repository.save(banner("비활성", 1, false, NOW.minusDays(1), NOW.plusDays(1)));

    // when
    List<BannerResponse> result = service.getActiveBanners(10);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("활성 배너의 시작 시각이 아직 오지 않았을 때 조회 결과에서 제외한다")
  void 시작_전_배너를_제외할_수_있다() {
    // given
    repository.save(banner("시작 전", 1, true, NOW.plusSeconds(1), NOW.plusDays(1)));

    // when
    List<BannerResponse> result = service.getActiveBanners(10);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("활성 배너의 종료 시각이 지났을 때 조회 결과에서 제외한다")
  void 만료된_배너를_제외할_수_있다() {
    // given
    repository.save(banner("만료", 1, true, NOW.minusDays(1), NOW.minusSeconds(1)));

    // when
    List<BannerResponse> result = service.getActiveBanners(10);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("활성 배너의 시작·종료 시각이 모두 비어 있을 때 조회 결과에 포함한다")
  void 기간_제한이_없는_활성_배너를_조회할_수_있다() {
    // given
    repository.save(banner("기간 없음", 1, true, null, null));

    // when
    List<BannerResponse> result = service.getActiveBanners(10);

    // then
    assertThat(result).extracting(BannerResponse::getName).containsExactly("기간 없음");
  }

  @Test
  @DisplayName("현재 시각이 시작 시각 또는 종료 시각과 같을 때 조회 결과에 포함한다")
  void 경계_시각의_배너를_조회할_수_있다() {
    // given
    repository.save(banner("시작 경계", 1, true, NOW, NOW.plusDays(1)));
    repository.save(banner("종료 경계", 2, true, NOW.minusDays(1), NOW));

    // when
    List<BannerResponse> result = service.getActiveBanners(10);

    // then
    assertThat(result).extracting(BannerResponse::getName).containsExactly("시작 경계", "종료 경계");
  }

  @Test
  @DisplayName("기간 밖 배너가 앞 순서에 있을 때 limit 전에 제외해 노출 가능한 배너를 채운다")
  void 기간_필터를_limit_전에_적용할_수_있다() {
    // given
    repository.save(banner("만료", 1, true, NOW.minusDays(2), NOW.minusDays(1)));
    repository.save(banner("시작 전", 2, true, NOW.plusDays(1), NOW.plusDays(2)));
    repository.save(banner("노출 A", 3, true, null, null));
    repository.save(banner("노출 B", 4, true, NOW.minusDays(1), NOW.plusDays(1)));
    repository.save(banner("노출 C", 5, true, null, null));

    // when
    List<BannerResponse> result = service.getActiveBanners(2);

    // then
    assertThat(result).extracting(BannerResponse::getName).containsExactly("노출 A", "노출 B");
  }

  private Banner banner(
      String name,
      int sortOrder,
      boolean isActive,
      LocalDateTime startDate,
      LocalDateTime endDate) {
    return Banner.builder()
        .name(name)
        .imageUrl("https://example.com/" + sortOrder + ".jpg")
        .bannerType(BannerType.CURATION)
        .sortOrder(sortOrder)
        .isActive(isActive)
        .startDate(startDate)
        .endDate(endDate)
        .build();
  }
}
