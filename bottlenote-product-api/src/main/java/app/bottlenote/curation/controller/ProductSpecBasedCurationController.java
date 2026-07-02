package app.bottlenote.curation.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.curation.service.ProductSpecBasedCurationService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/curations")
@RequiredArgsConstructor
@SecurityPolicy(auth = PUBLIC)
public class ProductSpecBasedCurationController {

  private final ProductSpecBasedCurationService productSpecBasedCurationService;

  @GetMapping
  public ResponseEntity<?> getCurations() {
    return GlobalResponse.ok(productSpecBasedCurationService.listActiveCurations());
  }

  @GetMapping("/feed")
  public ResponseEntity<?> getCurationFeed(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String code,
      @RequestParam(required = false, defaultValue = "0") Long cursor,
      @RequestParam(required = false, defaultValue = "10") Integer size) {
    return GlobalResponse.ok(
        productSpecBasedCurationService.searchFeed(keyword, code, cursor, size));
  }

  @GetMapping("/{curationId}")
  public ResponseEntity<?> getCuration(@PathVariable Long curationId) {
    return GlobalResponse.ok(productSpecBasedCurationService.getDetail(curationId));
  }
}
