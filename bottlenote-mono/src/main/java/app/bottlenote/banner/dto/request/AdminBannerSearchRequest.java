package app.bottlenote.banner.dto.request;

import app.bottlenote.banner.constant.BannerType;

public record AdminBannerSearchRequest(
    String keyword, Boolean isActive, BannerType bannerType, Integer page, Integer size) {

  public AdminBannerSearchRequest {
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
