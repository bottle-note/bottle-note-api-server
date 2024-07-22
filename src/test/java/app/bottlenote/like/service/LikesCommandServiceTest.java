package app.bottlenote.like.service;

import app.bottlenote.like.domain.LikeStatus;
import app.bottlenote.like.fixture.InmemoryLikesRepository;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.fixture.FakeUserDomainSupport;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import app.bottlenote.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static app.bottlenote.like.fixture.LikesObjectFixture.createFixtureReview;
import static app.bottlenote.like.fixture.LikesObjectFixture.createFixtureUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LikesCommandServiceTest {

	private static final Logger log = LoggerFactory.getLogger(LikesCommandServiceTest.class);
	private LikesCommandService likesCommandService;
	private InmemoryLikesRepository likesRepository;

	@BeforeEach
	void setUp() {

		User user_1 = createFixtureUser();
		User user_2 = createFixtureUser();

		FakeUserDomainSupport userDomainSupport = new FakeUserDomainSupport(user_1, user_2);
		InMemoryReviewRepository reviewRepository = new InMemoryReviewRepository();
		likesRepository = new InmemoryLikesRepository();

		likesCommandService = new LikesCommandService(userDomainSupport, reviewRepository, likesRepository);

		Review fixtureReview = createFixtureReview(user_1.getId(), 1L);
		reviewRepository.save(fixtureReview);
	}

	@Test
	@DisplayName("사용자는 리뷰에 좋아요를 할 수 있다.")
	void test_1() {
		var response = likesCommandService.updateLikes(1L, 1L, LikeStatus.LIKE);

		log.info("response = {}", response);


		//then
		likesRepository.findAll().stream().findFirst().ifPresent(
			likes -> {
				log.info("db에 저장된 likes = {}", likes);
				assertEquals(likes.getReview().getId(), 1L);
				assertEquals(likes.getUserInfo().getUserId(), 1L);
				assertEquals(likes.getStatus(), LikeStatus.LIKE);
			});

		assertNotNull(response);
		assertEquals(response.likesId(), 1L);
		assertEquals(response.reviewId(), 1L);
		assertEquals(response.userId(), 1L);
		assertEquals(response.status(), LikeStatus.LIKE);
	}

	@Test
	@DisplayName("이미 중복된 좋아요 요청이 들어와도 동일한 값을 반환한다. ")
	void test_2() {
		//given
		likesCommandService.updateLikes(1L, 1L, LikeStatus.LIKE);

		//when
		var response = likesCommandService.updateLikes(1L, 1L, LikeStatus.LIKE);

		//then
		assertNotNull(response);
		assertEquals(response.likesId(), 1L);
		assertEquals(response.reviewId(), 1L);
		assertEquals(response.userId(), 1L);
		assertEquals(response.status(), LikeStatus.LIKE);
	}

	@Test
	@DisplayName("좋아요 요청 후 좋아요 취소 요청을 할 수 있다.")
	void test_3() {
		//given
		likesCommandService.updateLikes(1L, 1L, LikeStatus.LIKE);

		//when
		var response = likesCommandService.updateLikes(1L, 1L, LikeStatus.DISLIKE);

		//then
		assertNotNull(response);
		assertEquals(response.likesId(), 1L);
		assertEquals(response.reviewId(), 1L);
		assertEquals(response.userId(), 1L);
		assertEquals(response.status(), LikeStatus.DISLIKE);
	}
}
