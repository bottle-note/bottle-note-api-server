package app.bottlenote.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.review.constant.ReviewSortType;
import app.bottlenote.review.dto.dsl.ReviewExploreCriteria;
import app.bottlenote.review.dto.request.ReviewExploreRequest;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import app.bottlenote.review.dto.response.ReviewExploreListResponse;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("ReviewExploreService 기본 정렬 단위 테스트")
class ReviewExploreServiceTest {

  @Test
  @DisplayName("정렬 파라미터가 없으면 최신 createAt 내림차순과 reviewId 내림차순으로 반환한다")
  void 정렬_파라미터가_없으면_최신순으로_반환한다() {
    // given
    LatestOnlyInMemoryReviewRepository repository =
        new LatestOnlyInMemoryReviewRepository(
            List.of(
                item(1L, "2026-08-15T09:00:00"),
                item(2L, "2026-08-16T09:00:00"),
                item(3L, "2026-08-16T09:00:00")));
    ReviewExploreService service = new ReviewExploreService(repository);
    ReviewExploreRequest request =
        new ReviewExploreRequest(null, null, null, null, null, null, null, null);

    // when
    KeysetPageResponse<ReviewExploreListResponse> result = service.getStandardExplore(request, 1L);

    // then
    assertThat(repository.criteria.sortType()).isEqualTo(ReviewSortType.LATEST);
    assertThat(result.content().items())
        .extracting(ReviewExploreItem::reviewId)
        .containsExactly(3L, 2L, 1L);
  }

  private static ReviewExploreItem item(Long reviewId, String createAt) {
    return new ReviewExploreItem(
        null,
        false,
        1L,
        "위스키",
        reviewId,
        "리뷰",
        4.0,
        List.of(),
        LocalDateTime.parse(createAt),
        LocalDateTime.parse(createAt),
        0L,
        List.of(),
        null,
        false,
        0L,
        false,
        0L,
        false);
  }

  private static final class LatestOnlyInMemoryReviewRepository extends InMemoryReviewRepository {
    private final List<ReviewExploreItem> state;
    private ReviewExploreCriteria criteria;

    private LatestOnlyInMemoryReviewRepository(List<ReviewExploreItem> state) {
      this.state = state;
    }

    @Override
    public KeysetPageResponse<ReviewExploreListResponse> getStandardExplore(
        ReviewExploreCriteria criteria) {
      this.criteria = criteria;
      if (criteria.sortType() != ReviewSortType.LATEST) {
        throw new AssertionError("무파라미터 리뷰 둘러보기는 최신순이어야 합니다.");
      }
      List<ReviewExploreItem> result =
          state.stream()
              .sorted(
                  Comparator.comparing(ReviewExploreItem::createAt)
                      .reversed()
                      .thenComparing(ReviewExploreItem::reviewId, Comparator.reverseOrder()))
              .toList();
      return KeysetPageResponse.of(
          new ReviewExploreListResponse(result), new KeysetPagination(false, null));
    }
  }
}
