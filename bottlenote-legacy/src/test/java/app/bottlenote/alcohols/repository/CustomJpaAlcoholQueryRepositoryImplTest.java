package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.constant.SearchSortType.REVIEW;
import static app.bottlenote.global.service.cursor.SortOrder.DESC;
import static app.bottlenote.user.constant.SocialType.GOOGLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.config.ModuleConfig;
import app.bottlenote.config.TestConfig;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.review.domain.Review;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Disabled("테스트 컨테이너 도입으로 인한 추후 수정 대상 ")
@Tag(value = "data-jpa-test")
@DisplayName("[database] [repository] AlcoholQuery")
@DataJpaTest
@ActiveProfiles("test")
@Import({TestConfig.class, ModuleConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomJpaAlcoholQueryRepositoryImplTest {

  private EntityManager em;
  @Autowired private TestEntityManager testEntityManager;
  @Autowired private AlcoholQueryRepository alcoholQueryRepository;
  @Autowired private UserRepository userRepository;

  static Stream<Arguments> testCase1Provider() {
    return Stream.of(
        Arguments.of("검색조건이 없어도 조회 된다.", AlcoholSearchRequest.builder().build(), "none"),
        Arguments.of(
            "키워드를 통해 검색 할 수 있다.",
            AlcoholSearchRequest.builder().keyword("아벨라워").build(),
            "keyword"),
        Arguments.of(
            "카테고리를 통해 검색 할 수 있다.",
            AlcoholSearchRequest.builder().category(AlcoholCategoryGroup.BLEND).build(),
            "category"),
        Arguments.of(
            "지역을 통해 검색 할 수 있다.",
            AlcoholSearchRequest.builder().regionId(5L).pageSize(1L).build(),
            "region"),
        Arguments.of(
            "정렬 조건을 지정할 수 있다.",
            AlcoholSearchRequest.builder().sortType(REVIEW).sortOrder(DESC).pageSize(2L).build(),
            "sort"),
        Arguments.of(
            "페이지 조건을 지정할 수 있다.",
            AlcoholSearchRequest.builder().cursor(5L).pageSize(8L).build(),
            "page"));
  }

  @BeforeEach
  void init() {
    em = testEntityManager.getEntityManager();

    Alcohol alcohol = alcoholQueryRepository.findById(1L).orElseThrow();
    User user =
        userRepository.save(
            User.builder()
                .email("test@emai.com")
                .nickName("test")
                .role(UserType.ROLE_USER)
                .socialType(new ArrayList<>(List.of(GOOGLE)))
                .build());

    Review review =
        Review.builder().alcoholId(alcohol.getId()).userId(user.getId()).content("맛있어요").build();
    RatingId ratingId = RatingId.is(alcohol.getId(), user.getId());
    Rating rating_1 = Rating.builder().id(ratingId).ratingPoint(RatingPoint.of(4.5)).build();

    em.persist(review);
    em.persist(rating_1);
  }

  @AfterEach
  void tearDown() {
    em.clear();
  }

  @Transactional(readOnly = true)
  @ParameterizedTest(name = "[{index}]{0}")
  @DisplayName("검색조건에 따라 술을 조회 할 수 있다.")
  @MethodSource("testCase1Provider")
  void test_case_1(String description, AlcoholSearchRequest request, String testType) {

    System.out.println(description);

    AlcoholSearchCriteria criteria = AlcoholSearchCriteria.of(request, null);

    // when
    PageResponse<AlcoholSearchResponse> response = alcoholQueryRepository.searchAlcohols(criteria);

    // then
    AlcoholSearchResponse content = response.content();
    List<AlcoholsSearchItem> alcohols = content.getAlcohols();
    Long totalCount = content.getTotalCount();

    assertNotNull(response);
    assertTrue(totalCount > 0);

    edgeTest(request, testType, alcohols, response);
  }

  private void edgeTest(
      AlcoholSearchRequest request,
      String testType,
      List<AlcoholsSearchItem> alcohols,
      PageResponse<AlcoholSearchResponse> response) {
    switch (testType) {
      case "keyword":
        assertTrue(
            alcohols.stream()
                .allMatch(
                    detail ->
                        detail.getKorName().contains(request.keyword())
                            || detail.getEngName().contains(request.keyword())));
        break;
      case "category":
        assertTrue(
            alcohols.stream()
                .allMatch(
                    detail -> request.category().containsCategory(detail.getEngCategoryName())));
        break;
      case "region":
        String regionAlcohol = "아란"; // 리전ID 5는 아란이 포함된 술이 있음
        assertTrue(
            alcohols.stream()
                .allMatch(
                    detail ->
                        detail.getKorName().contains(regionAlcohol)
                            || detail.getEngName().contains(regionAlcohol)));
        break;
      case "sort":
        System.out.println("test case sort");
        AlcoholsSearchItem detail_1 = alcohols.get(0);
        AlcoholsSearchItem detail_2 = alcohols.get(1);
        assertTrue(detail_1.getReviewCount() > detail_2.getReviewCount());
        break;
      case "page":
        System.out.println("test case page");
        CursorPageable pageable = response.cursorPageable();
        assertEquals(request.cursor(), pageable.getCurrentCursor());
        assertEquals(request.pageSize(), pageable.getPageSize());
        System.out.println(pageable);
        break;
    }
  }
}
