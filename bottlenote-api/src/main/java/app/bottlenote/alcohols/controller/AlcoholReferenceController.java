package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.controller.docs.AlcoholReferenceApiDocs;
import app.bottlenote.alcohols.service.AlcoholReferenceService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityPolicy(auth = PUBLIC)
@AlcoholReferenceApiDocs.ApiTag
public class AlcoholReferenceController {

  private final AlcoholReferenceService alcoholReferenceService;

  @AlcoholReferenceApiDocs.GetRegions
  @GetMapping("/regions")
  public ResponseEntity<GlobalResponse> findAll() {
    return GlobalResponse.ok(alcoholReferenceService.findAllRegion());
  }

  @AlcoholReferenceApiDocs.GetCategories
  @GetMapping("/alcohols/categories")
  public ResponseEntity<GlobalResponse> getAlcoholCategory(
      @RequestParam(required = false, defaultValue = "WHISKY") AlcoholType type) {
    return GlobalResponse.ok(alcoholReferenceService.getAlcoholCategory(type));
  }
}
