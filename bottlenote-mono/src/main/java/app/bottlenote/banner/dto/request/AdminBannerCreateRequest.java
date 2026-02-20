package app.bottlenote.banner.dto.request;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.constant.TextPosition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Builder;

public record AdminBannerCreateRequest(
    @NotBlank(message = "BANNER_NAME_REQUIRED") String name,
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "INVALID_HEX_COLOR_FORMAT")
        String nameFontColor,
    @Size(max = 50, message = "BANNER_DESCRIPTION_MAX_SIZE") String descriptionA,
    @Size(max = 50, message = "BANNER_DESCRIPTION_MAX_SIZE") String descriptionB,
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "INVALID_HEX_COLOR_FORMAT")
        String descriptionFontColor,
    @NotBlank(message = "BANNER_IMAGE_URL_REQUIRED") String imageUrl,
    TextPosition textPosition,
    Boolean isExternalUrl,
    String targetUrl,
    @NotNull(message = "BANNER_TYPE_REQUIRED") BannerType bannerType,
    @Min(value = 0, message = "BANNER_SORT_ORDER_MINIMUM") Integer sortOrder,
    LocalDateTime startDate,
    LocalDateTime endDate) {

  @Builder
  public AdminBannerCreateRequest {
    nameFontColor = nameFontColor != null ? nameFontColor : "#ffffff";
    descriptionFontColor = descriptionFontColor != null ? descriptionFontColor : "#ffffff";
    textPosition = textPosition != null ? textPosition : TextPosition.RT;
    isExternalUrl = isExternalUrl != null ? isExternalUrl : false;
    sortOrder = sortOrder != null ? sortOrder : 0;
  }
}
