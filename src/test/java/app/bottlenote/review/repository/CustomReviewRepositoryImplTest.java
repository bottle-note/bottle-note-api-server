package app.bottlenote.review.repository;

import static app.bottlenote.global.service.cursor.SortOrder.DESC;
import static app.bottlenote.like.domain.LikeStatus.LIKE;
import static app.bottlenote.review.domain.constant.ReviewSortType.RATING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.config.ModuleConfig;
import app.bottlenote.config.TestConfig;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.repository.UserCommandRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;
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

@Tag(value = "data-jpa-test")
@DataJpaTest
@ActiveProfiles("test")
@Import({TestConfig.class, ModuleConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomReviewRepositoryImplTest {

	@Autowired
	TestEntityManager testEntityManager;
	EntityManager em;

	@Autowired
	private AlcoholQueryRepository alcoholQueryRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private UserCommandRepository userRepository;


	static Stream<Arguments> testCase1Provider() {
		return Stream.of(
			Arguments.of("검색조건이 없어도 조회 된다.",
				PageableRequest.builder().build()
				, "none"
			), Arguments.of("정렬 조건을 지정할 수 있다.",
				PageableRequest.builder().sortType(RATING).sortOrder(DESC).pageSize(2L).build()
				, "sort"
			), Arguments.of("페이지 조건을 지정할 수 있다.",
				PageableRequest.builder().cursor(1L).pageSize(2L).build()
				, "page"
			)
		);
	}

	@BeforeEach
	void init() {
		em = testEntityManager.getEntityManager();

		Alcohol alcohol = alcoholQueryRepository.findById(1L).orElseThrow();
		User user = userRepository.save(User.builder().email("test@emai.com").nickName("test").role(
			UserType.ROLE_USER).socialType(SocialType.GOOGLE).build());
		User user2 = userRepository.save(
			User.builder().email("test2@emai.com").nickName("test2").role(
				UserType.ROLE_USER).socialType(SocialType.GOOGLE).build());

		Review review = Review.builder().alcoholId(alcohol.getId()).userId(user.getId()).address("서울시 강남구 압구정동")
			.content("맛있어요").build();
		Review review2 = Review.builder().alcoholId(alcohol.getId()).userId(user2.getId()).address("서울시 강남구 신사동")
			.content("그저 그래요").build();

		Likes likes = Likes.builder().user(user).review(review).status(LIKE).build();
		Likes likes2 = Likes.builder().user(user2).review(review).status(LIKE).build();
		Likes likes3 = Likes.builder().user(user2).review(review2).status(LIKE).build();

		em.persist(review);
		em.persist(review2);

		em.persist(likes);
		em.persist(likes2);
		em.persist(likes3);
	}

	@AfterEach
	void tearDown() {
		em.clear();
	}

	@Transactional(readOnly = true)
	@ParameterizedTest(name = "[{index}]{0}")
	@MethodSource("testCase1Provider")
	void test_case_1(String description, PageableRequest request, String testType) {

		System.out.println(description);

		PageableRequest pageableRequest = PageableRequest.builder().build();

		// when
		PageResponse<ReviewListResponse> response = reviewRepository.getReviews(1L, pageableRequest,
			1L);

		// then
		ReviewListResponse content = response.content();
		List<ReviewResponse> reviewList = content.getReviewList();
		Long totalCount = content.getTotalCount();

		assertNotNull(response);
		assertTrue(totalCount > 0);

		edgeTest(pageableRequest, testType, reviewList, response);
	}

	private void edgeTest(PageableRequest request, String testType, List<ReviewResponse> reviewList,
		PageResponse<ReviewListResponse> response) {
		switch (testType) {
			case "sort":
				System.out.println("test case sort");
				ReviewResponse reviewResponse = reviewList.get(0);
				ReviewResponse reviewResponse2 = reviewList.get(1);

				//좋아요 순 정렬
				assertTrue(reviewResponse.getLikeCount() > reviewResponse2.getLikeCount());
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
