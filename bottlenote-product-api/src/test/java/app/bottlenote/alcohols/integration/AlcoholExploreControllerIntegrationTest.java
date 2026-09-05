package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.dto.dsl.ExploreStandardCriteria;
import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * 위스키 둘러보기 컨트롤러({@code /api/v1/alcohols/explore/standard}) 통합 테스트.
 *
 * <p>검색({@link AlcoholQueryIntegrationTest})과는 별도 컨트롤러이므로 분리 관리한다. 시나리오별로 {@code @Nested} 그룹을 사용한다.
 */
@Tag("integration")
@DisplayName("[integration] [controller] AlcoholExplore")
class AlcoholExploreControllerIntegrationTest extends IntegrationTestSupport {

  private static final String ENDPOINT = "/api/v1/alcohols/explore/standard";

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private AlcoholQueryRepository alcoholQueryRepository;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private HmacCursorCodec cursorCodec;

  private MvcTestResult exchangeGet(
      java.util.function.Consumer<
              org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder>
          customizer) {
    var builder =
        mockMvcTester
            .get()
            .uri(ENDPOINT)
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + getToken())
            .with(csrf());
    customizer.accept(builder);
    return builder.exchange();
  }

  // =============================================================================================
  // 기본 동작
  // =============================================================================================

  @Nested
  @DisplayName("기본 동작")
  class DefaultBehavior {

    @Test
    @DisplayName("기본 호출 시 200과 필수 응답 필드가 반환된다")
    void explore_default() {
      alcoholTestFactory.persistAlcohols(5);

      MvcTestResult result = exchangeGet(b -> {});

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items")
          .asArray()
          .isNotEmpty();
      result.assertThat().bodyJson().extractingPath("$.meta.pagination.hasNext").isNotNull();
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.meta.searchParameters.sortType")
          .isEqualTo("RANDOM");
    }

    @Test
    @DisplayName("매칭 없는 키워드 조회 시 빈 items와 hasNext=false 반환")
    void explore_empty_result() {
      alcoholTestFactory.persistAlcohols(3);

      MvcTestResult result = exchangeGet(b -> b.param("keyword", "존재하지_않는_키워드_9999"));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items")
          .asArray()
          .isEmpty();
      result.assertThat().bodyJson().extractingPath("$.meta.pagination.hasNext").isEqualTo(false);
    }

    @Test
    @DisplayName("삭제 처리된 알코올은 Product 둘러보기에서 제외된다")
    void explore_excludes_deleted_alcohol() {
      Alcohol visible = alcoholTestFactory.persistAlcoholWithName("둘러보기 노출", "Explore Visible");
      Alcohol deleted = alcoholTestFactory.persistAlcoholWithName("둘러보기 삭제", "Explore Deleted");
      deleted.delete();
      alcoholQueryRepository.save(deleted);

      MvcTestResult result = exchangeGet(b -> b.param("keyword", "둘러보기").param("size", "10"));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .contains(visible.getId().intValue())
          .doesNotContain(deleted.getId().intValue());
    }

    @Test
    @DisplayName("응답 item에 reviewCount, pickCount 필드가 포함된다")
    void explore_response_includes_count_fields() {
      alcoholTestFactory.persistAlcohols(1);

      MvcTestResult result = exchangeGet(b -> {});

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[0].reviewCount")
          .isNotNull();
      result.assertThat().bodyJson().extractingPath("$.data.items[0].pickCount").isNotNull();
    }
  }

  // =============================================================================================
  // 입력 검증 (400)
  // =============================================================================================

  @Nested
  @DisplayName("입력 검증")
  class InputValidation {

    @Test
    @DisplayName("서명되지 않은 커서는 400을 반환한다")
    void rejects_invalid_cursor() {
      MvcTestResult result = exchangeGet(b -> b.param("cursor", "-1"));

      result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    }
  }

  // =============================================================================================
  // 필터
  // =============================================================================================

  @Nested
  @DisplayName("필터")
  class Filters {

    @Test
    @DisplayName("regionIds 복수 OR 필터: 지정되지 않은 지역은 제외된다")
    void filter_by_regionIds() {
      Region regionA = alcoholTestFactory.persistRegion("지역A", "Region A");
      Region regionB = alcoholTestFactory.persistRegion("지역B", "Region B");
      Region regionC = alcoholTestFactory.persistRegion("지역C", "Region C");
      Distillery distillery = alcoholTestFactory.persistDistillery("증류소", "Distillery");
      Alcohol inA = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, regionA, distillery);
      Alcohol inB = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, regionB, distillery);
      Alcohol inC = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, regionC, distillery);

      MvcTestResult result =
          exchangeGet(
              b ->
                  b.param("regionIds", String.valueOf(regionA.getId()))
                      .param("regionIds", String.valueOf(regionB.getId())));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .containsExactlyInAnyOrder(inA.getId().intValue(), inB.getId().intValue())
          .doesNotContain(inC.getId().intValue());
    }

    @Test
    @DisplayName("distilleryIds 복수 OR 필터: 지정되지 않은 증류소는 제외된다")
    void filter_by_distilleryIds() {
      Region region = alcoholTestFactory.persistRegion("지역A", "Region A");
      Distillery distA = alcoholTestFactory.persistDistillery("디스틸러리A", "Distillery A");
      Distillery distB = alcoholTestFactory.persistDistillery("디스틸러리B", "Distillery B");
      Distillery distC = alcoholTestFactory.persistDistillery("디스틸러리C", "Distillery C");
      alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region, distA);
      alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region, distB);
      alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region, distC);

      MvcTestResult result =
          exchangeGet(
              b ->
                  b.param("distilleryIds", String.valueOf(distA.getId()))
                      .param("distilleryIds", String.valueOf(distB.getId())));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].korDistillery")
          .asArray()
          .doesNotContain("디스틸러리C");
    }

    @Test
    @DisplayName("curationId 필터: 큐레이션에 포함된 알코올만 조회된다")
    void filter_by_curationId() {
      Alcohol a1 = alcoholTestFactory.persistAlcoholWithName("큐레이션A1", "Curation A1");
      Alcohol a2 = alcoholTestFactory.persistAlcoholWithName("큐레이션A2", "Curation A2");
      Alcohol excluded = alcoholTestFactory.persistAlcoholWithName("큐레이션외", "Out-of-curation");
      var curation = alcoholTestFactory.persistCurationKeyword("봄 추천 위스키", List.of(a1, a2));

      MvcTestResult result =
          exchangeGet(b -> b.param("curationId", String.valueOf(curation.getId())));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .contains(a1.getId().intValue(), a2.getId().intValue())
          .doesNotContain(excluded.getId().intValue());
    }

    @Test
    @DisplayName("단일 keyword를 토큰으로 분리해 AND로 결합한다")
    void filter_keyword_tokens_AND() {
      Alcohol both =
          alcoholTestFactory.persistAlcoholWithName("글렌피딕 시그니처", "Glenfiddich Signature");
      Alcohol onlyA = alcoholTestFactory.persistAlcoholWithName("글렌피딕 12", "Glenfiddich 12");
      Alcohol onlyB = alcoholTestFactory.persistAlcoholWithName("맥캘란 시그니처", "Macallan Signature");

      MvcTestResult result = exchangeGet(b -> b.param("keyword", "  글렌피딕   시그니처  "));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .contains(both.getId().intValue())
          .doesNotContain(onlyA.getId().intValue(), onlyB.getId().intValue());
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.meta.searchParameters.keyword")
          .isEqualTo("글렌피딕   시그니처");
      exchangeGet(b -> b.param("keyword", "시그니처 글렌피딕"))
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .contains(both.getId().intValue())
          .doesNotContain(onlyA.getId().intValue(), onlyB.getId().intValue());
    }

    @ParameterizedTest(name = "keyword={0}")
    @ValueSource(strings = {"%", "_"})
    @DisplayName("LIKE wildcard 문자는 검색 패턴이 아니라 일반 문자로 처리한다")
    void keyword_like_wildcard_is_literal(String wildcard) {
      alcoholTestFactory.persistAlcoholWithName("와일드카드 위스키", "Wildcard Whisky");

      exchangeGet(b -> b.param("keyword", wildcard))
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items")
          .asArray()
          .isEmpty();
    }
  }

  // =============================================================================================
  // 정렬
  // =============================================================================================

  @Nested
  @DisplayName("정렬")
  class Sort {

    @ParameterizedTest(name = "sortType={0}")
    @DisplayName("sortType 파라미터가 응답 meta.searchParameters.sortType에 반영된다")
    @EnumSource(SearchSortType.class)
    void sort_reflected_in_meta(SearchSortType sortType) {
      alcoholTestFactory.persistAlcohols(3);

      MvcTestResult result =
          exchangeGet(b -> b.param("sortType", sortType.name()).param("sortOrder", "DESC"));

      result
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.meta.searchParameters.sortType")
          .isEqualTo(sortType.name());
    }
  }

  // =============================================================================================
  // 페이징
  // =============================================================================================

  @Nested
  @DisplayName("페이징")
  class KeysetPagination {

    @Test
    @DisplayName("커서 기반 페이징 시 페이지 간 중복 데이터가 발생하지 않는다")
    void cursor_pagination_no_duplicates() throws Exception {
      alcoholTestFactory.persistAlcohols(15);

      MvcTestResult firstPage =
          exchangeGet(
              b -> b.param("size", "5").param("sortType", "RATING").param("sortOrder", "DESC"));
      String nextCursor =
          com.jayway.jsonpath.JsonPath.read(
              firstPage.getMvcResult().getResponse().getContentAsString(),
              "$.meta.pagination.nextCursor");

      MvcTestResult secondPage =
          exchangeGet(
              b ->
                  b.param("cursor", nextCursor)
                      .param("size", "5")
                      .param("sortType", "RATING")
                      .param("sortOrder", "DESC"));

      firstPage.assertThat().hasStatusOk();
      secondPage.assertThat().hasStatusOk();

      // 첫 페이지의 alcoholId 들을 JsonPath로 직접 추출해 두 번째 페이지에 중복되지 않음을 확인
      List<Integer> firstIds =
          com.jayway.jsonpath.JsonPath.read(
              firstPage.getMvcResult().getResponse().getContentAsString(),
              "$.data.items[*].alcoholId");
      secondPage
          .assertThat()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .doesNotContainAnyElementsOf(firstIds);
    }

    @Test
    @DisplayName("동일 keyword와 cursor로 다음 페이지를 중복 없이 조회한다")
    void keyword_cursor_continues_without_duplicates() throws Exception {
      alcoholTestFactory.persistAlcoholWithName("커서검색 위스키 A", "Cursor Whisky A");
      alcoholTestFactory.persistAlcoholWithName("커서검색 위스키 B", "Cursor Whisky B");
      Alcohol excluded = alcoholTestFactory.persistAlcoholWithName("다른 위스키", "Other Whisky");

      MvcTestResult first =
          exchangeGet(
              b ->
                  b.param("keyword", "커서검색")
                      .param("size", "1")
                      .param("sortType", "RATING")
                      .param("sortOrder", "DESC"));
      String nextCursor =
          com.jayway.jsonpath.JsonPath.read(
              first.getMvcResult().getResponse().getContentAsString(),
              "$.meta.pagination.nextCursor");

      MvcTestResult second =
          exchangeGet(
              b ->
                  b.param("keyword", "커서검색")
                      .param("cursor", nextCursor)
                      .param("size", "1")
                      .param("sortType", "RATING")
                      .param("sortOrder", "DESC"));

      List<Integer> firstIds =
          com.jayway.jsonpath.JsonPath.read(
              first.getMvcResult().getResponse().getContentAsString(),
              "$.data.items[*].alcoholId");
      second
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .isNotEmpty()
          .doesNotContainAnyElementsOf(firstIds)
          .doesNotContain(excluded.getId().intValue());
    }
  }

  // =============================================================================================
  // 정렬 커서 안정성
  // =============================================================================================

  /** 정렬별 커서가 최초 조회 기준과 동점 순서를 유지하는지 검증한다. */
  @Nested
  @DisplayName("정렬 커서 안정성")
  class SortCursorStability {

    @Test
    @DisplayName("nextCursor로 이어 요청하면 첫 페이지와 중복되지 않는다")
    void next_cursor_continues_without_overlap() throws Exception {
      alcoholTestFactory.persistAlcohols(20);

      MvcTestResult first = exchangeGet(b -> b.param("sortType", "RANDOM").param("size", "5"));
      String nextCursor =
          com.jayway.jsonpath.JsonPath.read(
              first.getMvcResult().getResponse().getContentAsString(),
              "$.meta.pagination.nextCursor");
      MvcTestResult second =
          exchangeGet(
              b -> b.param("sortType", "RANDOM").param("cursor", nextCursor).param("size", "5"));

      List<Integer> firstIds =
          com.jayway.jsonpath.JsonPath.read(
              first.getMvcResult().getResponse().getContentAsString(), "$.data.items[*].alcoholId");
      second
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .doesNotContainAnyElementsOf(firstIds);
    }

    @Test
    @DisplayName("비-RANDOM 정렬(POPULAR)은 같은 조건에서 같은 순서를 유지한다")
    void non_random_sort_is_stable() throws Exception {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(10);
      LocalDateTime bucketAt = BucketGranularity.HOUR.startAt(LocalDateTime.now());
      for (int i = 0; i < alcohols.size(); i++) {
        alcoholTestFactory.persistPopularitySnapshot(
            alcohols.get(i).getId(),
            BucketGranularity.HOUR,
            bucketAt,
            BigDecimal.ZERO,
            BigDecimal.valueOf(i + 1, 1));
      }

      MvcTestResult first =
          exchangeGet(
              b -> b.param("sortType", "POPULAR").param("sortOrder", "DESC").param("size", "10"));
      MvcTestResult second =
          exchangeGet(
              b -> b.param("sortType", "POPULAR").param("sortOrder", "DESC").param("size", "10"));

      List<Integer> firstIds =
          com.jayway.jsonpath.JsonPath.read(
              first.getMvcResult().getResponse().getContentAsString(), "$.data.items[*].alcoholId");
      second
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .containsExactlyElementsOf(firstIds);
    }

    @Test
    @DisplayName("HOUR Snapshot이 없어도 POPULAR 목록은 전체 주류를 반환한다")
    void popular_without_snapshot_returns_all_alcohols() throws Exception {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(3);

      exchangeGet(b -> b.param("sortType", "POPULAR").param("sortOrder", "DESC").param("size", "3"))
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .containsExactly(
              alcohols.get(0).getId().intValue(),
              alcohols.get(1).getId().intValue(),
              alcohols.get(2).getId().intValue());
    }

    @Test
    @DisplayName("31개에만 Snapshot이 있어도 POPULAR과 RANDOM은 34개 ID를 누락과 중복 없이 반환한다")
    void partial_snapshot_keeps_same_ids_across_sorts_and_page_sizes() throws Exception {
      String keyword = "개수일치";
      List<Alcohol> alcohols = persistNamedAlcohols(keyword, 34);
      LocalDateTime bucket = BucketGranularity.HOUR.startAt(LocalDateTime.now()).minusHours(1);
      for (int index = 0; index < 31; index++) {
        alcoholTestFactory.persistPopularitySnapshot(
            alcohols.get(index).getId(),
            BucketGranularity.HOUR,
            bucket,
            BigDecimal.ZERO,
            BigDecimal.valueOf(index + 1L, 2));
      }
      Set<Integer> expectedIds = new HashSet<>(alcoholIds(alcohols));

      for (int size : List.of(10, 20)) {
        List<Integer> popularIds =
            fetchAllIds(keyword, SearchSortType.POPULAR, SortOrder.DESC, size);
        List<Integer> randomIds =
            fetchAllIds(keyword, SearchSortType.RANDOM, SortOrder.DESC, size);

        assertCompleteIdSet(popularIds, expectedIds);
        assertCompleteIdSet(randomIds, expectedIds);
        assertThat(new HashSet<>(popularIds)).isEqualTo(new HashSet<>(randomIds));
      }
    }

    @ParameterizedTest(name = "sortOrder={0}")
    @EnumSource(SortOrder.class)
    @DisplayName("POPULAR은 실제 0점과 Snapshot이 없는 주류를 ID 오름차순 동점으로 페이징한다")
    void popular_zero_score_tie_uses_id_ascending(SortOrder sortOrder) throws Exception {
      String keyword = "0점동점";
      Alcohol actualZero =
          alcoholTestFactory.persistAlcoholWithName(keyword + " Snapshot", "Zero Snapshot");
      Alcohol withoutSnapshot =
          alcoholTestFactory.persistAlcoholWithName(keyword + " Missing", "Zero Missing");
      Alcohol positive =
          alcoholTestFactory.persistAlcoholWithName(keyword + " Positive", "Positive");
      LocalDateTime bucket = BucketGranularity.HOUR.startAt(LocalDateTime.now()).minusHours(1);
      alcoholTestFactory.persistPopularitySnapshot(
          actualZero.getId(),
          BucketGranularity.HOUR,
          bucket,
          BigDecimal.ZERO,
          BigDecimal.ZERO);
      alcoholTestFactory.persistPopularitySnapshot(
          positive.getId(),
          BucketGranularity.HOUR,
          bucket,
          BigDecimal.ZERO,
          BigDecimal.ONE);

      List<Integer> ids = fetchAllIds(keyword, SearchSortType.POPULAR, sortOrder, 1);

      List<Integer> zeroTieIds =
          List.of(actualZero.getId().intValue(), withoutSnapshot.getId().intValue());
      if (sortOrder == SortOrder.ASC) {
        assertThat(ids).containsExactlyElementsOf(
            List.of(zeroTieIds.get(0), zeroTieIds.get(1), positive.getId().intValue()));
      } else {
        assertThat(ids).containsExactlyElementsOf(
            List.of(positive.getId().intValue(), zeroTieIds.get(0), zeroTieIds.get(1)));
      }
    }

    @Test
    @DisplayName("Snapshot 부재 커서는 페이지 중간에 첫 Snapshot이 생겨도 0점 기준을 유지한다")
    void no_snapshot_cursor_keeps_zero_score_baseline() throws Exception {
      String keyword = "무스냅샷커서";
      List<Alcohol> alcohols = persistNamedAlcohols(keyword, 5);
      MvcTestResult first =
          exchangeGet(
              b ->
                  b.param("keyword", keyword)
                      .param("sortType", "POPULAR")
                      .param("sortOrder", "DESC")
                      .param("size", "2"));
      String firstCursor = nextCursor(first);
      assertThat(cursorCodec.verify(firstCursor, popularContext(keyword, SortOrder.DESC, 2)).extra())
          .containsEntry("bucketAt", ExploreStandardCriteria.NO_POPULARITY_BUCKET);

      LocalDateTime bucket = BucketGranularity.HOUR.startAt(LocalDateTime.now());
      for (int index = 0; index < alcohols.size(); index++) {
        alcoholTestFactory.persistPopularitySnapshot(
            alcohols.get(index).getId(),
            BucketGranularity.HOUR,
            bucket,
            BigDecimal.ZERO,
            BigDecimal.valueOf(alcohols.size() - index));
      }

      MvcTestResult second =
          exchangeGet(
              b ->
                  b.param("keyword", keyword)
                      .param("sortType", "POPULAR")
                      .param("sortOrder", "DESC")
                      .param("cursor", firstCursor)
                      .param("size", "2"));
      String secondCursor = nextCursor(second);
      MvcTestResult third =
          exchangeGet(
              b ->
                  b.param("keyword", keyword)
                      .param("sortType", "POPULAR")
                      .param("sortOrder", "DESC")
                      .param("cursor", secondCursor)
                      .param("size", "2"));

      List<Integer> ids = new ArrayList<>();
      ids.addAll(alcoholIds(first));
      ids.addAll(alcoholIds(second));
      ids.addAll(alcoholIds(third));
      assertThat(ids).containsExactlyElementsOf(alcoholIds(alcohols));
      third.assertThat().bodyJson().extractingPath("$.meta.pagination.hasNext").isEqualTo(false);
    }

    @Test
    @DisplayName("부분 Snapshot 상태에서도 POPULAR은 평점 범위와 삭제 필터를 유지한다")
    void popular_partial_snapshot_keeps_rating_and_deleted_filters() throws Exception {
      String keyword = "부분스냅샷필터";
      User ratingUser = userTestFactory.persistUser();
      Alcohol withSnapshot =
          alcoholTestFactory.persistAlcoholWithName(keyword + " Included Snapshot", "Included A");
      Alcohol withoutSnapshot =
          alcoholTestFactory.persistAlcoholWithName(keyword + " Included Missing", "Included B");
      Alcohol belowRange =
          alcoholTestFactory.persistAlcoholWithName(keyword + " Below", "Below");
      Alcohol deleted =
          alcoholTestFactory.persistAlcoholWithName(keyword + " Deleted", "Deleted");
      ratingTestFactory.persistRating(ratingUser, withSnapshot, 4);
      ratingTestFactory.persistRating(ratingUser, withoutSnapshot, 4);
      ratingTestFactory.persistRating(ratingUser, belowRange, 2);
      ratingTestFactory.persistRating(ratingUser, deleted, 5);
      LocalDateTime bucket = BucketGranularity.HOUR.startAt(LocalDateTime.now()).minusHours(1);
      alcoholTestFactory.persistPopularitySnapshot(
          withSnapshot.getId(),
          BucketGranularity.HOUR,
          bucket,
          BigDecimal.ZERO,
          BigDecimal.ONE);
      alcoholTestFactory.persistPopularitySnapshot(
          belowRange.getId(),
          BucketGranularity.HOUR,
          bucket,
          BigDecimal.ZERO,
          BigDecimal.TEN);
      deleted.delete();
      alcoholQueryRepository.save(deleted);

      exchangeGet(
              b ->
                  b.param("keyword", keyword)
                      .param("sortType", "POPULAR")
                      .param("sortOrder", "DESC")
                      .param("ratingFrom", "4.0")
                      .param("ratingTo", "5.0")
                      .param("size", "10"))
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .containsExactly(withSnapshot.getId().intValue(), withoutSnapshot.getId().intValue())
          .doesNotContain(belowRange.getId().intValue(), deleted.getId().intValue());
    }

    @ParameterizedTest(name = "bucketAt={0}")
    @ValueSource(strings = {"MISSING", "", " ", "not-a-date"})
    @DisplayName("서명된 POPULAR 커서의 bucketAt이 누락되거나 형식이 틀리면 400을 반환한다")
    void popular_cursor_rejects_invalid_bucket_at(String bucketAt) {
      getToken();
      String context = popularContext(null, SortOrder.DESC, 1);
      Map<String, String> extra =
          "MISSING".equals(bucketAt) ? Map.of() : Map.of("bucketAt", bucketAt);
      String cursor =
          cursorCodec.encode(context, Map.of("id", "1", "sort", "0"), extra);

      exchangeGet(
              b ->
                  b.param("sortType", "POPULAR")
                      .param("sortOrder", "DESC")
                      .param("cursor", cursor)
                      .param("size", "1"))
          .assertThat()
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyJson()
          .extractingPath("$.errors[0].code")
          .isEqualTo("INVALID_CURSOR");
    }

    @Test
    @DisplayName("POPULAR 커서는 최초 HOUR Snapshot 버킷을 다음 페이지까지 유지한다")
    void popular_cursor_keeps_first_snapshot_bucket() throws Exception {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(6);
      LocalDateTime firstBucket = BucketGranularity.HOUR.startAt(LocalDateTime.now()).minusHours(1);
      for (int i = 0; i < alcohols.size(); i++) {
        alcoholTestFactory.persistPopularitySnapshot(
            alcohols.get(i).getId(),
            BucketGranularity.HOUR,
            firstBucket,
            BigDecimal.ZERO,
            BigDecimal.valueOf(i + 1, 1));
      }

      MvcTestResult first =
          exchangeGet(
              b -> b.param("sortType", "POPULAR").param("sortOrder", "DESC").param("size", "3"));
      String nextCursor =
          com.jayway.jsonpath.JsonPath.read(
              first.getMvcResult().getResponse().getContentAsString(),
              "$.meta.pagination.nextCursor");
      List<Integer> firstIds =
          com.jayway.jsonpath.JsonPath.read(
              first.getMvcResult().getResponse().getContentAsString(), "$.data.items[*].alcoholId");
      assertThat(firstIds)
          .containsExactly(
              alcohols.get(5).getId().intValue(),
              alcohols.get(4).getId().intValue(),
              alcohols.get(3).getId().intValue());

      LocalDateTime nextBucket = firstBucket.plusHours(1);
      for (int i = 0; i < alcohols.size(); i++) {
        alcoholTestFactory.persistPopularitySnapshot(
            alcohols.get(i).getId(),
            BucketGranularity.HOUR,
            nextBucket,
            BigDecimal.ZERO,
            BigDecimal.valueOf(alcohols.size() - i, 1));
      }

      MvcTestResult second =
          exchangeGet(
              b ->
                  b.param("sortType", "POPULAR")
                      .param("sortOrder", "DESC")
                      .param("cursor", nextCursor)
                      .param("size", "3"));

      second
          .assertThat()
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .containsExactly(
              alcohols.get(2).getId().intValue(),
              alcohols.get(1).getId().intValue(),
              alcohols.get(0).getId().intValue());
    }

    @Test
    @DisplayName("POPULAR ASC는 동점 점수를 ID 오름차순으로 끊어 다음 페이지를 이어간다")
    void popular_asc_cursor_keeps_tie_break_order() throws Exception {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(5);
      LocalDateTime bucket = BucketGranularity.HOUR.startAt(LocalDateTime.now()).minusHours(1);
      List<BigDecimal> scores =
          List.of(
              new BigDecimal("0.1"),
              new BigDecimal("0.2"),
              new BigDecimal("0.2"),
              new BigDecimal("0.2"),
              new BigDecimal("0.3"));
      for (int i = 0; i < alcohols.size(); i++) {
        alcoholTestFactory.persistPopularitySnapshot(
            alcohols.get(i).getId(),
            BucketGranularity.HOUR,
            bucket,
            BigDecimal.ZERO,
            scores.get(i));
      }

      MvcTestResult first =
          exchangeGet(
              b -> b.param("sortType", "POPULAR").param("sortOrder", "ASC").param("size", "2"));
      String cursor =
          com.jayway.jsonpath.JsonPath.read(
              first.getMvcResult().getResponse().getContentAsString(),
              "$.meta.pagination.nextCursor");

      first
          .assertThat()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .containsExactly(alcohols.get(0).getId().intValue(), alcohols.get(1).getId().intValue());
      exchangeGet(
              b ->
                  b.param("sortType", "POPULAR")
                      .param("sortOrder", "ASC")
                      .param("cursor", cursor)
                      .param("size", "2"))
          .assertThat()
          .bodyJson()
          .extractingPath("$.data.items[*].alcoholId")
          .asArray()
          .containsExactly(alcohols.get(2).getId().intValue(), alcohols.get(3).getId().intValue());
    }
  }

  private List<Alcohol> persistNamedAlcohols(String keyword, int count) {
    List<Alcohol> alcohols = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      alcohols.add(
          alcoholTestFactory.persistAlcoholWithName(
              keyword + " " + index, "Explore Regression " + index));
    }
    return alcohols;
  }

  private List<Integer> fetchAllIds(
      String keyword, SearchSortType sortType, SortOrder sortOrder, int size) throws Exception {
    List<Integer> ids = new ArrayList<>();
    Set<String> seenCursors = new HashSet<>();
    String cursor = null;
    int pageCount = 0;
    boolean hasNext;
    do {
      assertThat(++pageCount).as("페이지 순회가 종료되어야 한다").isLessThanOrEqualTo(100);
      String currentCursor = cursor;
      MvcTestResult page =
          exchangeGet(
              b -> {
                b.param("keyword", keyword)
                    .param("sortType", sortType.name())
                    .param("sortOrder", sortOrder.name())
                    .param("size", String.valueOf(size));
                if (currentCursor != null) {
                  b.param("cursor", currentCursor);
                }
              });
      page.assertThat().hasStatusOk();
      ids.addAll(alcoholIds(page));
      hasNext = readJsonPath(page, "$.meta.pagination.hasNext");
      cursor = hasNext ? nextCursor(page) : null;
      if (hasNext) {
        assertThat(cursor).isNotBlank();
        assertThat(seenCursors.add(cursor)).as("다음 커서가 반복되지 않아야 한다").isTrue();
      }
    } while (hasNext);
    return ids;
  }

  private String popularContext(String keyword, SortOrder sortOrder, int size) {
    ExploreStandardRequest request =
        ExploreStandardRequest.builder()
            .keyword(keyword)
            .sortType(SearchSortType.POPULAR)
            .sortOrder(sortOrder)
            .size(size)
            .build();
    return ExploreStandardCriteria.of(request, getTokenUserId(), 0L, null).context();
  }

  private void assertCompleteIdSet(List<Integer> actualIds, Set<Integer> expectedIds) {
    assertThat(actualIds).hasSize(expectedIds.size());
    assertThat(new HashSet<>(actualIds))
        .hasSize(actualIds.size())
        .containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  private List<Integer> alcoholIds(List<Alcohol> alcohols) {
    return alcohols.stream().map(alcohol -> alcohol.getId().intValue()).toList();
  }

  private List<Integer> alcoholIds(MvcTestResult result) throws Exception {
    return readJsonPath(result, "$.data.items[*].alcoholId");
  }

  private String nextCursor(MvcTestResult result) throws Exception {
    return readJsonPath(result, "$.meta.pagination.nextCursor");
  }

  private <T> T readJsonPath(MvcTestResult result, String path) throws Exception {
    return com.jayway.jsonpath.JsonPath.read(
        result.getMvcResult().getResponse().getContentAsString(), path);
  }
}
