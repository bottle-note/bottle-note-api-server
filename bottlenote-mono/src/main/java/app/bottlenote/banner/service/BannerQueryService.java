package app.bottlenote.banner.service;

import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.domain.BannerRepository;
import app.bottlenote.banner.dto.response.BannerResponse;
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

  @Transactional(readOnly = true)
  public List<BannerResponse> getActiveBanners(Integer limit) {
    return bannerRepository.findAllByIsActiveTrue().stream()
        .sorted(Comparator.comparing(Banner::getSortOrder))
        .limit(limit)
        .map(
            banner ->
                BannerResponse.builder()
                    .id(banner.getId())
                    .name(banner.getName())
                    .description(banner.getDescription())
                    .imageUrl(banner.getImageUrl())
                    .textPosition(banner.getTextPosition())
                    .targetUrl(banner.getTargetUrl())
                    .isExternalUrl(banner.getIsExternalUrl())
                    .bannerType(banner.getBannerType())
                    .sortOrder(banner.getSortOrder())
                    .startDate(banner.getStartDate())
                    .endDate(banner.getEndDate())
                    .build())
        .toList();
  }
}
