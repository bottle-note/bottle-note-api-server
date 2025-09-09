package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;
import static app.bottlenote.shared.meta.MetaService.createMetaInfo;

import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.data.response.GlobalResponse;
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
public class AlcoholQueryController {

  private final AlcoholQueryService alcoholQueryService;

  @GetMapping("/search")
  public ResponseEntity<?> searchAlcohols(@ModelAttribute @Valid AlcoholSearchRequest request) {
    Long id = getUserIdByContext().orElse(-1L);
    PageResponse<AlcoholSearchResponse> pageResponse =
        alcoholQueryService.searchAlcohols(request, id);
    return GlobalResponse.ok(
        pageResponse.content(),
        createMetaInfo()
            .add("searchParameters", request)
            .add("pageable", pageResponse.cursorPageable()));
  }

  @GetMapping("/{alcoholId}")
  public ResponseEntity<?> findAlcoholDetailById(@PathVariable Long alcoholId) {
    Long id = getUserIdByContext().orElse(-1L);
    return GlobalResponse.ok(alcoholQueryService.findAlcoholDetailById(alcoholId, id));
  }
}
