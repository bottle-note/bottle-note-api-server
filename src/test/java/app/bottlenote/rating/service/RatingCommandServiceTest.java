package app.bottlenote.rating.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.exception.RatingException;
import app.bottlenote.rating.fixture.InMemoryRatingRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.fixture.InMemoryUserQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@Tag("unit")
@DisplayName("[unit] [service] RatingCommandService")
class RatingCommandServiceTest {
	private final Long userId = 1L;
	private final Long alcoholId = 1L;
	private RatingRepository ratingRepository;
	private RatingCommandService ratingCommandService;
	private User user;
	private Alcohol alcohol;

	@BeforeEach
	void setup() {
		this.ratingRepository = new InMemoryRatingRepository();
		UserQueryRepository userQueryRepository = new InMemoryUserQueryRepository();
		AlcoholQueryRepository alcoholQueryRepository = new InMemoryAlcoholQueryRepository();

		this.ratingCommandService = new RatingCommandService(ratingRepository, userQueryRepository, alcoholQueryRepository);

		user = User.builder().id(userId).build();
		alcohol = Alcohol.builder().id(alcoholId).build();
		userQueryRepository.save(user);
		alcoholQueryRepository.save(alcohol);
	}

	@Nested
	@DisplayName("별점 등록")
	class RegisterRatingPoint {

		@Test
		@DisplayName("신규 별점을 등록할 수 있다.")
		void test_1() {
			//given
			RatingPoint ratingPoint = RatingPoint.of(5);
			//when
			RatingRegisterResponse register = ratingCommandService.register(alcoholId, userId, ratingPoint);
			//then
			assertNotNull(register);
			assertEquals(register.rating(), ratingPoint.toString());
		}

		@Test
		@DisplayName("기존 별점을 수정 할 수 있다.")
		void test_2() {
			//given
			ratingRepository.save(Rating.builder()
				.id(RatingId.is(userId, alcoholId))
				.alcohol(alcohol)
				.user(user)
				.ratingPoint(RatingPoint.of(1))
				.build());

			RatingPoint ratingPoint = RatingPoint.of(5);

			//when
			String 변경전_포인트 = ratingRepository.findByAlcoholIdAndUserId(alcoholId, userId).get().getRatingPoint().getRating().toString();
			RatingRegisterResponse register = ratingCommandService.register(alcoholId, userId, ratingPoint);
			String 변경후_포인트 = register.rating();
			//then.
			assertNotNull(register);
			assertNotEquals(변경전_포인트, 변경후_포인트);
			assertEquals(변경후_포인트, ratingPoint.getRating().toString());
		}

		@Test
		@DisplayName("필수 파라미터가  없을 경우 NullPointerException을 발생시킨다.")
		void test_3() {
			//when  && then
			RatingPoint point = RatingPoint.of(5);
			assertThrows(NullPointerException.class, () -> ratingCommandService.register(null, userId, point));
			assertThrows(NullPointerException.class, () -> ratingCommandService.register(alcoholId, null, point));
			assertThrows(NullPointerException.class, () -> ratingCommandService.register(alcoholId, userId, null));
		}

		@Test
		@DisplayName("유저가 없을 경우 UserException을 발생시킨다.")
		void test_4() {
			//when  && then
			assertThrows(UserException.class, () -> ratingCommandService.register(alcoholId, 22L, RatingPoint.of(5)));
		}

		@Test
		@DisplayName("알코올이 없을 경우 RatingException 발생시킨다.")
		void test_5() {
			//when  && then
			assertThrows(RatingException.class, () -> ratingCommandService.register(22L, userId, RatingPoint.of(5)));
		}
	}
}
