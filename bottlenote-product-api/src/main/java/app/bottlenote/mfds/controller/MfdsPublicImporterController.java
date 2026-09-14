package app.bottlenote.mfds.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.mfds.controller.docs.MfdsPublicApiDocs;
import app.bottlenote.mfds.dto.request.MfdsPublicImporterSearchRequest;
import app.bottlenote.mfds.dto.response.MfdsPublicImporterItem;
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
@RequestMapping("/api/v1/mfds/importers")
@SecurityPolicy(auth = PUBLIC)
@MfdsPublicApiDocs.ApiTag
public class MfdsPublicImporterController {

  private final MfdsPublicQueryService mfdsPublicQueryService;

  @MfdsPublicApiDocs.SearchImporters
  @GetMapping
  public ResponseEntity<GlobalResponse> searchImporters(
      @Valid @ModelAttribute MfdsPublicImporterSearchRequest request) {
    KeysetPageResponse<List<MfdsPublicImporterItem>> page =
        mfdsPublicQueryService.searchImporters(request);
    return GlobalResponse.ok(
        page.content(),
        MetaService.createMetaInfo()
            .add("searchParameters", request)
            .add("pagination", page.pagination()));
  }

  @MfdsPublicApiDocs.GetImporter
  @GetMapping("/{importerId}")
  public ResponseEntity<GlobalResponse> getImporter(@PathVariable Long importerId) {
    return GlobalResponse.ok(mfdsPublicQueryService.getImporter(importerId));
  }
}
