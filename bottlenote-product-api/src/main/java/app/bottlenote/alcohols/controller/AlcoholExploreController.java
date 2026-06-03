package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;

import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.ExploreStandardResponse;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
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
@SecurityPolicy(auth = OPTIONAL_AUTH)
public class AlcoholExploreController {

  private final AlcoholQueryService alcoholQueryService;

  /** 기본 표준 형태의 위스키 둘러보기 API. 검색 기능(필터/정렬)을 흡수한 메인 탐색 엔드포인트. */
  @GetMapping("/standard")
  public ResponseEntity<?> getStandardExplore(
      @ModelAttribute @Valid ExploreStandardRequest request) {
    Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

    ExploreStandardResponse result = alcoholQueryService.getStandardExplore(request, userId);

    CollectionResponse<AlcoholDetailItem> data = CollectionResponse.of(0L, result.page());
    MetaInfos meta = MetaService.createMetaInfo();
    meta.add("pageable", result.page().pageable());
    meta.add("searchParameters", request);
    meta.add("seed", result.seed());
    return GlobalResponse.ok(data, meta);
  }
}
