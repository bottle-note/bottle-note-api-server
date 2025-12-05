package app.bottlenote.banner.dto.response;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.constant.TextPosition;
import java.time.LocalDateTime;
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
  private String nameFontColor;
  private String descriptionA;
  private String descriptionB;
  private String descriptionFontColor;
  private String imageUrl;
  private TextPosition textPosition;
  private String targetUrl;
  private Boolean isExternalUrl;
  private BannerType bannerType;
  private Integer sortOrder;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
}
