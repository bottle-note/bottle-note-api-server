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
    @NotBlank(message = "배너명은 필수입니다.") String name,
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "HEX 색상 형식이 올바르지 않습니다.") String nameFontColor,
    @Size(max = 50, message = "배너 설명은 최대 50자까지 가능합니다.") String descriptionA,
    @Size(max = 50, message = "배너 설명은 최대 50자까지 가능합니다.") String descriptionB,
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "HEX 색상 형식이 올바르지 않습니다.")
        String descriptionFontColor,
    @NotBlank(message = "이미지 URL은 필수입니다.") String imageUrl,
    TextPosition textPosition,
    Boolean isExternalUrl,
    String targetUrl,
    @NotNull(message = "배너 유형은 필수입니다.") BannerType bannerType,
    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.") Integer sortOrder,
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
