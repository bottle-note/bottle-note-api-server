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
