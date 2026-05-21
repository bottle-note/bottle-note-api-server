package app.bottlenote.curation.controller;

import app.bottlenote.curation.service.ProductSpecBasedCurationService;
import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/curations")
@RequiredArgsConstructor
public class ProductSpecBasedCurationController {

  private final ProductSpecBasedCurationService productSpecBasedCurationService;

  @GetMapping
  public ResponseEntity<?> getCurations() {
    return GlobalResponse.ok(productSpecBasedCurationService.listActiveCurations());
  }

  @GetMapping("/{curationId}")
  public ResponseEntity<?> getCuration(@PathVariable Long curationId) {
    return GlobalResponse.ok(productSpecBasedCurationService.getDetail(curationId));
  }
}
