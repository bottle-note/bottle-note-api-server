package app.bottlenote.curation.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.curation.service.CurationSpecQueryService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/curation-specs")
@RequiredArgsConstructor
@SecurityPolicy(auth = PUBLIC)
public class ProductCurationSpecController {

  private final CurationSpecQueryService curationSpecQueryService;

  @GetMapping
  public ResponseEntity<?> getCurationSpecs() {
    return GlobalResponse.ok(curationSpecQueryService.listActiveSpecs());
  }

  @GetMapping("/{specId}")
  public ResponseEntity<?> getCurationSpec(@PathVariable Long specId) {
    return GlobalResponse.ok(curationSpecQueryService.getActiveSpecDetail(specId));
  }
}
