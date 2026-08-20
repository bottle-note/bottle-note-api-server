package app.bottlenote.review.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.review.controller.docs.ReviewExploreApiDocs;
import app.bottlenote.review.dto.request.ReviewExploreRequest;
import app.bottlenote.review.dto.response.ReviewExploreListResponse;
import app.bottlenote.review.service.ReviewExploreService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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
@RequestMapping("/api/v1/reviews/explore")
@SecurityPolicy(auth = OPTIONAL_AUTH)
@ReviewExploreApiDocs.ApiTag
public class ReviewExploreController {

  private final ReviewExploreService reviewExploreService;

  @ReviewExploreApiDocs.GetStandardExplore
  @GetMapping("/standard")
  @Parameters({
    @Parameter(name = "keyword", description = "단일 검색어. keywords와 함께 보낼 수 없습니다."),
    @Parameter(
        name = "keywords",
        description = "제거 예정인 legacy 다중 검색어. keyword가 없을 때만 적용됩니다.",
        deprecated = true),
    @Parameter(name = "sortType", description = "POPULAR, LIKES, RATING, BOTTLE_PRICE, GLASS_PRICE"),
    @Parameter(name = "sortOrder", description = "ASC 또는 DESC. 기본값은 DESC입니다."),
    @Parameter(name = "rating", description = "0.5부터 5.0까지 0.5 단위의 작성 평점 필터")
  })
  public ResponseEntity<GlobalResponse> getStandardExplore(
      @ModelAttribute @Valid ReviewExploreRequest request) {
    Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
    PageResponse<ReviewExploreListResponse> page =
        reviewExploreService.getStandardExplore(request, userId);
    return GlobalResponse.ok(
        page.content(),
        MetaService.createMetaInfo()
            .add("searchParameters", request)
            .add("pagination", page.pagination()));
  }
}
