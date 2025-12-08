package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;
import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;

import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.PageResponse;
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

    // 키워드에 따라 큐레이션 ID 매핑 임시 ( 나중에는 이벤트 시작 시점에 로딩해오는게 더 좋을듯
    request =
        switch (request.keyword() != null && !request.keyword().isEmpty()
            ? request.keyword()
            : "") {
          case "비 오는 날 추천 위스키" -> request.convertCurationId(1L);
          case "강렬한 폭풍우 피트 추천 위스키" -> request.convertCurationId(2L);
          case "가을 추천 위스키" -> request.convertCurationId(3L);
          default -> request;
        };

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
