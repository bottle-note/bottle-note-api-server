package app.bottlenote.banner.dto.response;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.constant.TextPosition;
import app.bottlenote.banner.domain.Banner;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {

  private Long id;
  private String name;
  private String imageUrl;
  private TextPosition textPosition;
  private String targetUrl;
  private Boolean isExternalUrl;
  private BannerType bannerType;
  private Integer sortOrder;
  private LocalDate startDate;
  private LocalDate endDate;

  public static BannerResponse from(Banner banner) {
    return BannerResponse.builder()
        .id(banner.getId())
        .name(banner.getName())
        .imageUrl(banner.getImageUrl())
        .textPosition(banner.getTextPosition())
        .targetUrl(banner.getTargetUrl())
        .isExternalUrl(banner.getIsExternalUrl())
        .bannerType(banner.getBannerType())
        .sortOrder(banner.getSortOrder())
        .startDate(banner.getStartDate())
        .endDate(banner.getEndDate())
        .build();
  }
}
