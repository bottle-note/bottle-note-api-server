package app.bottlenote.banner.fixture;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.constant.TextPosition;
import app.bottlenote.banner.domain.Banner;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Banner 엔티티 테스트 팩토리
 *
 * <p>테스트에서 Banner 엔티티를 생성하고 영속화하는 헬퍼 클래스
 */
@Component
public class BannerTestFactory {

  @PersistenceContext private EntityManager em;

  /** 기본 Banner 생성 */
  @Transactional
  @NotNull
  public Banner persistBanner(@NotNull String name, @NotNull String imageUrl) {
    Banner banner =
        Banner.builder()
            .name(name)
            .imageUrl(imageUrl)
            .textPosition(TextPosition.CENTER)
            .bannerType(BannerType.CURATION)
            .sortOrder(0)
            .isActive(true)
            .build();

    em.persist(banner);
    em.flush();
    return banner;
  }

  /** 상세 설정 Banner 생성 */
  @Transactional
  @NotNull
  public Banner persistBanner(
      @NotNull String name,
      @NotNull String imageUrl,
      @NotNull TextPosition textPosition,
      @NotNull BannerType bannerType,
      @NotNull Integer sortOrder,
      @NotNull Boolean isActive) {
    Banner banner =
        Banner.builder()
            .name(name)
            .imageUrl(imageUrl)
            .textPosition(textPosition)
            .bannerType(bannerType)
            .sortOrder(sortOrder)
            .isActive(isActive)
            .build();

    em.persist(banner);
    em.flush();
    return banner;
  }

  /** 빌더를 사용한 Banner 생성 */
  @Transactional
  @NotNull
  public Banner persistBanner(@NotNull Banner.BannerBuilder builder) {
    Banner tempBanner = builder.build();
    Banner.BannerBuilder finalBuilder = fillMissingBannerFields(tempBanner, builder);

    Banner banner = finalBuilder.build();
    em.persist(banner);
    em.flush();
    return banner;
  }

  /** 여러 Banner 생성 (정렬 순서대로) */
  @Transactional
  @NotNull
  public List<Banner> persistMultipleBanners(int count) {
    List<Banner> banners = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Banner banner =
          Banner.builder()
              .name("배너 " + (i + 1))
              .imageUrl("https://example.com/banner" + (i + 1) + ".jpg")
              .textPosition(TextPosition.CENTER)
              .bannerType(BannerType.CURATION)
              .sortOrder(i)
              .isActive(true)
              .build();

      em.persist(banner);
      banners.add(banner);
    }
    em.flush();
    return banners;
  }

  /** 활성/비활성 배너 혼합 생성 */
  @Transactional
  @NotNull
  public List<Banner> persistMixedActiveBanners(int activeCount, int inactiveCount) {
    List<Banner> banners = new ArrayList<>();

    for (int i = 0; i < activeCount; i++) {
      Banner banner =
          Banner.builder()
              .name("활성 배너 " + (i + 1))
              .imageUrl("https://example.com/active" + (i + 1) + ".jpg")
              .textPosition(TextPosition.CENTER)
              .bannerType(BannerType.CURATION)
              .sortOrder(i)
              .isActive(true)
              .build();

      em.persist(banner);
      banners.add(banner);
    }

    for (int i = 0; i < inactiveCount; i++) {
      Banner banner =
          Banner.builder()
              .name("비활성 배너 " + (i + 1))
              .imageUrl("https://example.com/inactive" + (i + 1) + ".jpg")
              .textPosition(TextPosition.CENTER)
              .bannerType(BannerType.AD)
              .sortOrder(activeCount + i)
              .isActive(false)
              .build();

      em.persist(banner);
      banners.add(banner);
    }

    em.flush();
    return banners;
  }

  /** 기간이 설정된 Banner 생성 */
  @Transactional
  @NotNull
  public Banner persistBannerWithPeriod(
      @NotNull String name,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate) {
    Banner banner =
        Banner.builder()
            .name(name)
            .imageUrl("https://example.com/" + name + ".jpg")
            .textPosition(TextPosition.CENTER)
            .bannerType(BannerType.SURVEY)
            .sortOrder(0)
            .startDate(startDate)
            .endDate(endDate)
            .isActive(true)
            .build();

    em.persist(banner);
    em.flush();
    return banner;
  }

  private Banner.BannerBuilder fillMissingBannerFields(
      Banner tempBanner, Banner.BannerBuilder builder) {
    if (tempBanner.getName() == null) {
      builder.name("테스트 배너");
    }
    if (tempBanner.getImageUrl() == null) {
      builder.imageUrl("https://example.com/default.jpg");
    }
    if (tempBanner.getTextPosition() == null) {
      builder.textPosition(TextPosition.CENTER);
    }
    if (tempBanner.getBannerType() == null) {
      builder.bannerType(BannerType.CURATION);
    }
    return builder;
  }
}
