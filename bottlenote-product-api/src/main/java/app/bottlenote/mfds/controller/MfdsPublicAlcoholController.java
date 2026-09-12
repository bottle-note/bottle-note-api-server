package app.bottlenote.mfds.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.mfds.controller.docs.MfdsPublicApiDocs;
import app.bottlenote.mfds.dto.request.MfdsPublicAlcoholSearchRequest;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholListItem;
import app.bottlenote.mfds.service.MfdsPublicQueryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/mfds")
@SecurityPolicy(auth = PUBLIC)
@MfdsPublicApiDocs.ApiTag
public class MfdsPublicAlcoholController {

  private final MfdsPublicQueryService mfdsPublicQueryService;

  @MfdsPublicApiDocs.SearchAlcohols
  @GetMapping("/alcohols")
  public ResponseEntity<GlobalResponse> searchAlcohols(
      @Valid @ModelAttribute MfdsPublicAlcoholSearchRequest request) {
    KeysetPageResponse<List<MfdsPublicAlcoholListItem>> page =
        mfdsPublicQueryService.searchAlcohols(request);
    return GlobalResponse.ok(
        page.content(),
        MetaService.createMetaInfo()
            .add("searchParameters", request)
            .add("pagination", page.pagination()));
  }

  @MfdsPublicApiDocs.GetAlcohol
  @GetMapping("/alcohols/{id}")
  public ResponseEntity<GlobalResponse> getAlcohol(@PathVariable Long id) {
    return GlobalResponse.ok(mfdsPublicQueryService.getAlcohol(id));
  }

  @MfdsPublicApiDocs.ListCountries
  @GetMapping("/countries")
  public ResponseEntity<GlobalResponse> listCountries() {
    return GlobalResponse.ok(mfdsPublicQueryService.listCountries());
  }
}
