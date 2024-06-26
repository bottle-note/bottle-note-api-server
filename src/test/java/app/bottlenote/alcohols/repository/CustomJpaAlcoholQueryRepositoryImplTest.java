package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.config.ModuleConfig;
import app.bottlenote.config.TestConfig;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.review.domain.Review;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.repository.UserCommandRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.stream.Stream;

import static app.bottlenote.alcohols.domain.constant.SearchSortType.REVIEW;
import static app.bottlenote.global.service.cursor.SortOrder.DESC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(value = "data-jpa-test")
@DataJpaTest
@ActiveProfiles("test")
@Import({TestConfig.class, ModuleConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomJpaAlcoholQueryRepositoryImplTest {

	private EntityManager em;
	@Autowired
	private TestEntityManager testEntityManager;
	@Autowired
	private AlcoholQueryRepository alcoholQueryRepository;
	@Autowired
	private UserCommandRepository userRepository;

	static Stream<Arguments> testCase1Provider() {
		return Stream.of(
			Arguments.of("검색조건이 없어도 조회 된다.",
				AlcoholSearchRequest.builder().build()
				, "none"
			), Arguments.of("키워드를 통해 검색 할 수 있다.",
				AlcoholSearchRequest.builder().keyword("아벨라워").build()
				, "keyword"
			), Arguments.of("카테고리를 통해 검색 할 수 있다.",
				AlcoholSearchRequest.builder().category("싱글 몰트").build()
				, "category"
			), Arguments.of("지역을 통해 검색 할 수 있다.",
				AlcoholSearchRequest.builder().regionId(5L).pageSize(1L).build()
				, "region"
			), Arguments.of("정렬 조건을 지정할 수 있다.",
				AlcoholSearchRequest.builder().sortType(REVIEW).sortOrder(DESC).pageSize(2L).build()
				, "sort"
			), Arguments.of("페이지 조건을 지정할 수 있다.",
				AlcoholSearchRequest.builder().cursor(5L).pageSize(8L).build()
				, "page"
			)
		);
	}

	@BeforeEach
	void init() {
		em = testEntityManager.getEntityManager();

		Alcohol alcohol = alcoholQueryRepository.findById(1L).orElseThrow();
		User user = userRepository.save(User.builder().email("test@emai.com").nickName("test").role(UserType.ROLE_USER).socialType(SocialType.GOOGLE).build());

		Review review = Review.builder().alcoholId(alcohol.getId()).userId(user.getId()).address("서울시 강남구").content("맛있어요").build();
		RatingId ratingId = RatingId.is(alcohol.getId(), user.getId());
		Rating rating_1 = Rating.builder().id(ratingId).alcohol(alcohol).user(user).ratingPoint(RatingPoint.of(4.5)).build();

		em.persist(review);
		em.persist(rating_1);
	}

	@AfterEach
	void tearDown() {
		em.clear();
	}

	@Transactional(readOnly = true)
	@ParameterizedTest(name = "[{index}]{0}")
	@MethodSource("testCase1Provider")
	void test_case_1(String description, AlcoholSearchRequest request, String testType) {

		System.out.println(description);

		AlcoholSearchCriteria criteria = AlcoholSearchCriteria.of(request, null);

		// when
		PageResponse<AlcoholSearchResponse> response = alcoholQueryRepository.searchAlcohols(criteria);

		// then
		AlcoholSearchResponse content = response.content();
		List<AlcoholsSearchDetail> alcohols = content.getAlcohols();
		Long totalCount = content.getTotalCount();

		assertNotNull(response);
		assertTrue(totalCount > 0);

		edgeTest(request, testType, alcohols, response);
	}

	private void edgeTest(AlcoholSearchRequest request, String testType, List<AlcoholsSearchDetail> alcohols, PageResponse<AlcoholSearchResponse> response) {
		switch (testType) {
			case "keyword":
				assertTrue(alcohols.stream().allMatch(
					detail ->
						detail.getKorName().contains(request.keyword()) || detail.getEngName().contains(request.keyword())
				));
				break;
			case "category":
				assertTrue(alcohols.stream().allMatch(
					detail ->
						detail.getKorCategoryName().contains(request.category()) || detail.getEngCategoryName().contains(request.category())
				));
				break;
			case "region":
				String regionAlcohol = "아란"; // 리전ID 5는 아란이 포함된 술이 있음
				assertTrue(alcohols.stream().allMatch(
					detail ->
						detail.getKorName().contains(regionAlcohol) || detail.getEngName().contains(regionAlcohol)
				));
				break;
			case "sort":
				System.out.println("test case sort");
				AlcoholsSearchDetail detail_1 = alcohols.get(0);
				AlcoholsSearchDetail detail_2 = alcohols.get(1);
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
