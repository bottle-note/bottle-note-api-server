package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;

import app.bottlenote.alcohols.controller.docs.AlcoholExploreApiDocs;
import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.alcohols.dto.response.ExploreStandardResponse;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.meta.MetaService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
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
@AlcoholExploreApiDocs.ApiTag
public class AlcoholExploreController {

  private final AlcoholQueryService alcoholQueryService;

  /** 기본 표준 형태의 위스키 둘러보기 API. 검색 기능(필터/정렬)을 흡수한 메인 탐색 엔드포인트. */
  @AlcoholExploreApiDocs.GetStandardExplore
  @GetMapping("/standard")
  @Parameters({
    @Parameter(
        name = "ratingFrom",
        description = "표시 집계 평점 포함 하한. 0.5부터 5.0까지 0.5 단위입니다.",
        schema = @Schema(type = "number", minimum = "0.5", maximum = "5.0", multipleOf = 0.5)),
    @Parameter(
        name = "ratingTo",
        description = "표시 집계 평점 포함 상한. 0.5부터 5.0까지 0.5 단위입니다.",
        schema = @Schema(type = "number", minimum = "0.5", maximum = "5.0", multipleOf = 0.5))
  })
  public ResponseEntity<GlobalResponse> getStandardExplore(
      @ModelAttribute @Valid ExploreStandardRequest request) {
    Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
    KeysetPageResponse<ExploreStandardResponse> page =
        alcoholQueryService.getStandardExplore(request, userId);
    return GlobalResponse.ok(
        page.content(),
        MetaService.createMetaInfo()
            .add("searchParameters", request)
            .add("pagination", page.pagination()));
  }
}
