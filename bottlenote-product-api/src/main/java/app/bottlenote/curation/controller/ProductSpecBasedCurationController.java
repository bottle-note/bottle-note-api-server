package app.bottlenote.curation.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.curation.controller.docs.SpecBasedCurationApiDocs;
import app.bottlenote.curation.dto.request.CurationFeedSearchRequest;
import app.bottlenote.curation.dto.response.CurationFeedListResponse;
import app.bottlenote.curation.service.ProductSpecBasedCurationService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.service.meta.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/curations")
@RequiredArgsConstructor
@SecurityPolicy(auth = PUBLIC)
@SpecBasedCurationApiDocs.ApiTag
public class ProductSpecBasedCurationController {

  private final ProductSpecBasedCurationService productSpecBasedCurationService;

  @SpecBasedCurationApiDocs.GetCurations
  @GetMapping
  public ResponseEntity<GlobalResponse> getCurations() {
    return GlobalResponse.ok(productSpecBasedCurationService.listActiveCurations());
  }

  @SpecBasedCurationApiDocs.GetCurationFeed
  @GetMapping("/feed")
  public ResponseEntity<GlobalResponse> getCurationFeed(
      @ModelAttribute @Valid CurationFeedSearchRequest request) {
    KeysetPageResponse<CurationFeedListResponse> page = productSpecBasedCurationService.searchFeed(request);
    return GlobalResponse.ok(
        page.content(), MetaService.createMetaInfo().add("pagination", page.pagination()));
  }

  @SpecBasedCurationApiDocs.GetCuration
  @GetMapping("/{curationId}")
  public ResponseEntity<GlobalResponse> getCuration(@PathVariable Long curationId) {
    return GlobalResponse.ok(productSpecBasedCurationService.getDetail(curationId));
  }
}
