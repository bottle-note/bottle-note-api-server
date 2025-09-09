package app.bottlenote.alcohols.controller;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.service.AlcoholReferenceService;
import app.bottlenote.shared.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AlcoholReferenceController {

  private final AlcoholReferenceService alcoholReferenceService;

  @GetMapping("/regions")
  public ResponseEntity<?> findAll() {
    return GlobalResponse.ok(alcoholReferenceService.findAllRegion());
  }

  @GetMapping("/alcohols/categories")
  public ResponseEntity<?> getAlcoholCategory(
      @RequestParam(required = false, defaultValue = "WHISKY") AlcoholType type) {
    return GlobalResponse.ok(alcoholReferenceService.getAlcoholCategory(type));
  }
}
