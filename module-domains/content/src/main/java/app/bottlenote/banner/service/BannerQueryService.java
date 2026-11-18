package app.bottlenote.banner.service;

import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.domain.BannerRepository;
import app.bottlenote.banner.dto.response.BannerResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerQueryService {

  private final BannerRepository bannerRepository;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<BannerResponse> getActiveBanners(Integer limit) {
    LocalDateTime now = LocalDateTime.now(clock);
    return bannerRepository.findAllByIsActiveTrue().stream()
        .filter(banner -> isInDisplayPeriod(banner, now))
        .sorted(Comparator.comparing(Banner::getSortOrder))
        .limit(limit)
        .map(
            banner ->
                BannerResponse.builder()
                    .id(banner.getId())
                    .name(banner.getName())
                    .nameFontColor(banner.getNameFontColor())
                    .descriptionA(banner.getDescriptionA())
                    .descriptionB(banner.getDescriptionB())
                    .descriptionFontColor(banner.getDescriptionFontColor())
                    .imageUrl(banner.getImageUrl())
                    .posterUrl(banner.getPosterUrl())
                    .textPosition(banner.getTextPosition())
                    .targetUrl(banner.getTargetUrl())
                    .isExternalUrl(banner.getIsExternalUrl())
                    .mediaType(banner.getMediaType())
                    .bannerType(banner.getBannerType())
                    .sortOrder(banner.getSortOrder())
                    .startDate(banner.getStartDate())
                    .endDate(banner.getEndDate())
                    .build())
        .toList();
  }

  // 서버 시각 기준, null 이면 해당 쪽 제한 없음, 시작·종료 시각 경계 포함
  private boolean isInDisplayPeriod(Banner banner, LocalDateTime now) {
    boolean started = banner.getStartDate() == null || !now.isBefore(banner.getStartDate());
    boolean notEnded = banner.getEndDate() == null || !now.isAfter(banner.getEndDate());
    return started && notEnded;
  }
}
