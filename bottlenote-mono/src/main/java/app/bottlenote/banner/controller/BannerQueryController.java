package app.bottlenote.banner.controller;

import app.bottlenote.banner.service.BannerQueryService;
import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerQueryController {

  private final BannerQueryService bannerQueryService;

  @GetMapping
  public ResponseEntity<GlobalResponse> getActiveBanners(
      @RequestParam(defaultValue = "10") Integer limit) {
    return ResponseEntity.ok(
        GlobalResponse.success(bannerQueryService.getActiveBanners(limit)));
  }
}
