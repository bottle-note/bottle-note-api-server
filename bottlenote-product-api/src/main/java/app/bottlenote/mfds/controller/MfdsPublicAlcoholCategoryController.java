package app.bottlenote.mfds.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.mfds.controller.docs.MfdsPublicApiDocs;
import app.bottlenote.mfds.service.MfdsPublicQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/mfds/alcohols/category")
@SecurityPolicy(auth = PUBLIC)
@MfdsPublicApiDocs.ApiTag
public class MfdsPublicAlcoholCategoryController {

  private final MfdsPublicQueryService mfdsPublicQueryService;

  @MfdsPublicApiDocs.ListAlcoholCategories
  @GetMapping
  public ResponseEntity<GlobalResponse> getAlcoholCategories() {
    return GlobalResponse.ok(mfdsPublicQueryService.listAlcoholCategories());
  }
}
