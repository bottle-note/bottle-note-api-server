package app.bottlenote.rating.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.global.pagination.CursorProperties;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.response.AlcoholRatingStatsResponse;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import app.bottlenote.user.fixture.FakeUserFacade;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("RatingQueryService 단위 테스트")
class RatingQueryServiceTest {

  private HmacCursorCodec cursorCodec;
  private CapturingRatingRepository ratingRepository;
  private RatingQueryService ratingQueryService;

  @BeforeEach
  void setUp() {
    CursorProperties properties = new CursorProperties();
    properties.setCurrentKeyId("v1");
    properties.setCurrentSecret("test-pagination-cursor-secret");
    cursorCodec = new HmacCursorCodec(properties, Clock.systemUTC());
    ratingRepository = new CapturingRatingRepository();
    ratingQueryService =
        new RatingQueryService(
            ratingRepository, new FakeUserFacade(), new NoOpAlcoholFacade(), cursorCodec);
  }

  @Test
  @DisplayName("RANDOM 정렬이고 커서가 없으면 호출마다 새 seed를 생성한다")
  void fetchRatingList_whenRandomWithoutCursor_generatesNewSeedEachTime() {
    // given
    RatingListFetchRequest request =
        RatingListFetchRequest.builder().sortType(SearchSortType.RANDOM).build();

    // when
    ratingQueryService.fetchRatingList(request, 1L);
    long firstSeed = ratingRepository.lastCriteria.seed();
    ratingQueryService.fetchRatingList(request, 1L);
    long secondSeed = ratingRepository.lastCriteria.seed();

    // then
    assertThat(firstSeed).isNotEqualTo(secondSeed);
  }

  @Test
  @DisplayName("RANDOM 정렬이고 커서가 있으면 커서 extra에 담긴 seed를 그대로 이어받는다")
  void fetchRatingList_whenRandomWithCursor_reusesSeedFromCursorExtra() {
    // given - 첫 페이지 호출로 서버가 생성한 seed를 확보한다
    RatingListFetchRequest firstRequest =
        RatingListFetchRequest.builder().sortType(SearchSortType.RANDOM).build();
    ratingQueryService.fetchRatingList(firstRequest, 1L);
    long firstSeed = ratingRepository.lastCriteria.seed();
    String context = ratingRepository.lastCriteria.context();

    String cursor =
        cursorCodec.encode(
            context, Map.of("id", "10", "sort", "999"), Map.of("seed", String.valueOf(firstSeed)));
    RatingListFetchRequest secondRequest =
        RatingListFetchRequest.builder().sortType(SearchSortType.RANDOM).cursor(cursor).build();

    // when
    ratingQueryService.fetchRatingList(secondRequest, 1L);

    // then
    assertThat(ratingRepository.lastCriteria.seed()).isEqualTo(firstSeed);
  }

  @Test
  @DisplayName("RANDOM이 아닌 정렬은 커서 유무와 무관하게 seed가 항상 0이다")
  void fetchRatingList_whenNotRandom_seedIsAlwaysZero() {
    // given
    RatingListFetchRequest request =
        RatingListFetchRequest.builder()
            .sortType(SearchSortType.RATING)
            .sortOrder(SortOrder.DESC)
            .build();

    // when
    ratingQueryService.fetchRatingList(request, 1L);

    // then
    assertThat(ratingRepository.lastCriteria.seed()).isEqualTo(0L);
  }

  /** RatingRepository 테스트 더블 - fetchRatingList에 전달된 criteria를 그대로 캡처한다 */
  private static class CapturingRatingRepository implements RatingRepository {
    private RatingListFetchCriteria lastCriteria;

    @Override
    public Rating save(Rating rating) {
      throw new UnsupportedOperationException("not used in this test");
    }

    @Override
    public Optional<Rating> findById(RatingId ratingId) {
      return Optional.empty();
    }

    @Override
    public List<Rating> findAll() {
      return List.of();
    }

    @Override
    public List<Rating> findAllByIdIn(List<RatingId> ids) {
      return List.of();
    }

    @Override
    public Optional<Rating> findByAlcoholIdAndUserId(Long alcoholId, Long userId) {
      return Optional.empty();
    }

    @Override
    public KeysetPageResponse<RatingListFetchResponse> fetchRatingList(
        RatingListFetchCriteria criteria) {
      this.lastCriteria = criteria;
      return KeysetPageResponse.of(
          RatingListFetchResponse.create(List.of()), new KeysetPagination(false, null));
    }

    @Override
    public Optional<UserRatingResponse> fetchUserRating(Long alcoholId, Long userId) {
      return Optional.empty();
    }

    @Override
    public Double findAverageRatingByAlcoholId(Long alcoholId) {
      return 0.0;
    }

    @Override
    public Long countByAlcoholId(Long alcoholId) {
      return 0L;
    }

    @Override
    public List<AlcoholRatingStatsResponse> findStatsByAlcoholIds(List<Long> alcoholIds) {
      return List.of();
    }

    @Override
    public boolean existsByAlcoholId(Long alcoholId) {
      return false;
    }
  }

  /** AlcoholFacade 테스트 더블 - seed 해석 경로에서는 호출되지 않는다 */
  private static class NoOpAlcoholFacade implements AlcoholFacade {
    @Override
    public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long currentUserId) {
      return Optional.empty();
    }

    @Override
    public Pair<AlcoholSummaryItem, AlcoholSummaryItem> getAlcoholSummaryItemWithNext(
        Long alcoholId) {
      return null;
    }

    @Override
    public Boolean existsByAlcoholId(Long alcoholId) {
      return false;
    }

    @Override
    public void isValidAlcoholId(Long alcoholId) {}

    @Override
    public Optional<String> findAlcoholImageUrlById(Long alcoholId) {
      return Optional.empty();
    }
  }
}
