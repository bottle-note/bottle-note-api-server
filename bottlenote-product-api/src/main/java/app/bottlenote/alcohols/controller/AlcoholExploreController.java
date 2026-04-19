package app.bottlenote.alcohols.controller;

import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.meta.MetaInfos;
import app.bottlenote.global.service.meta.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/alcohols/explore")
public class AlcoholExploreController {

  private final AlcoholQueryService alcoholQueryService;

  /** 기본 표준 형태의 위스키 둘러보기 API. 검색 기능(필터/정렬)을 흡수한 메인 탐색 엔드포인트. */
  @GetMapping("/standard")
  public ResponseEntity<?> getStandardExplore(
      @ModelAttribute @Valid ExploreStandardRequest request) {
    Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

    CursorResponse<AlcoholDetailItem> result =
        alcoholQueryService.getStandardExplore(request, userId);

    CollectionResponse<AlcoholDetailItem> data = CollectionResponse.of(0L, result);
    MetaInfos meta = MetaService.createMetaInfo();
    meta.add("pageable", result.pageable());
    meta.add("searchParameters", request);
    return GlobalResponse.ok(data, meta);
  }
}
