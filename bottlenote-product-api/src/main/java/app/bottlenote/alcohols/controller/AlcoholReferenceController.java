package app.bottlenote.alcohols.controller;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.dto.request.CurationKeywordSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordResponse;
import app.bottlenote.alcohols.service.AlcoholReferenceService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.CursorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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

  @GetMapping("/curations")
  public ResponseEntity<?> searchCurationKeywords(
      @ModelAttribute CurationKeywordSearchRequest request) {
    CursorResponse<CurationKeywordResponse> response =
        alcoholReferenceService.searchCurationKeywords(request);
    return GlobalResponse.ok(response);
  }

  @GetMapping("/curations/{curationId}/alcohols")
  public ResponseEntity<?> getCurationAlcohols(
      @PathVariable Long curationId,
      @RequestParam(required = false, defaultValue = "0") Long cursor,
      @RequestParam(required = false, defaultValue = "10") Long pageSize) {
    CursorResponse<AlcoholsSearchItem> response =
        alcoholReferenceService.getCurationAlcohols(curationId, cursor, pageSize);
    return GlobalResponse.ok(response);
  }
}
