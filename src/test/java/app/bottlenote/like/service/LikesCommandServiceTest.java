package app.bottlenote.like.service;

import app.bottlenote.like.domain.LikeStatus;
import app.bottlenote.like.fixture.InmemoryLikesRepository;
import app.bottlenote.review.fixture.FakeReviewFacade;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.FakeUserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static app.bottlenote.like.fixture.LikesObjectFixture.createFixtureUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@Tag("unit")
@DisplayName("[unit] [service] LikesCommand")
class LikesCommandServiceTest {

	private static final Logger log = LoggerFactory.getLogger(LikesCommandServiceTest.class);
	private LikesCommandService likesCommandService;
	private InmemoryLikesRepository likesRepository;

	@BeforeEach
	void setUp() {

		User user1 = createFixtureUser();
		User user2 = createFixtureUser();

		FakeUserFacade userFacade = new FakeUserFacade(user1, user2);
		FakeReviewFacade reviewFacade = new FakeReviewFacade();
		likesRepository = new InmemoryLikesRepository();
		likesCommandService = new LikesCommandService(userFacade, reviewFacade, likesRepository);
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
				assertEquals(1L, likes.getReviewId());
				assertEquals(1L, likes.getUserInfo().getUserId());
				assertEquals(LikeStatus.LIKE, likes.getStatus());
			});

		assertNotNull(response);
		assertEquals(1L, response.likesId());
		assertEquals(1L, response.reviewId());
		assertEquals(1L, response.userId());
		assertEquals(LikeStatus.LIKE, response.status());
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
		assertEquals(1L, response.likesId());
		assertEquals(1L, response.reviewId());
		assertEquals(1L, response.userId());
		assertEquals(LikeStatus.LIKE, response.status());
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
		assertEquals(1L, response.likesId());
		assertEquals(1L, response.reviewId());
		assertEquals(1L, response.userId());
		assertEquals(LikeStatus.DISLIKE, response.status());
	}
}
