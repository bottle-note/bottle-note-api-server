package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;
import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;
import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;

import app.bottlenote.alcohols.controller.docs.AlcoholQueryApiDocs;
import app.bottlenote.alcohols.dto.request.AlcoholLookupRequest;
import app.bottlenote.alcohols.service.AlcoholLookupService;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/alcohols")
@SecurityPolicy(auth = OPTIONAL_AUTH)
@AlcoholQueryApiDocs.ApiTag
public class AlcoholQueryController {

  private final AlcoholQueryService alcoholQueryService;
  private final AlcoholLookupService alcoholLookupService;

  @AlcoholQueryApiDocs.GetLookups
  @GetMapping("/lookup")
  public ResponseEntity<GlobalResponse> getAlcoholLookups(
      @ModelAttribute @Valid AlcoholLookupRequest request) {
    var page = alcoholLookupService.lookup(request);
    return GlobalResponse.ok(
        page.content(),
        createMetaInfo().add("searchParameters", request).add("pagination", page.pagination()));
  }

  @AlcoholQueryApiDocs.GetAlcoholDetail
  @GetMapping("/{alcoholId}")
  public ResponseEntity<GlobalResponse> findAlcoholDetailById(@PathVariable Long alcoholId) {
    Long id = getUserIdByContext().orElse(-1L);
    return GlobalResponse.ok(alcoholQueryService.findAlcoholDetailById(alcoholId, id));
  }
}
