package app.bottlenote.rating.service;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.repository.JpaAlcoholQueryRepository;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.fixture.InMemoryRatingRepository;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.fixture.InMemoryUserQueryRepository;
import app.bottlenote.user.repository.JpaUserQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;


class RatingCommandServiceTest {
	private final RatingRepository ratingRepository;
	private final UserQueryRepository userQueryRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;


	public RatingCommandServiceTest(
		RatingRepository ratingRepository,
		JpaUserQueryRepository jpaUserQueryRepository,
		JpaAlcoholQueryRepository jpaAlcoholQueryRepository) {
		this.ratingRepository = new InMemoryRatingRepository();
		this.userQueryRepository = new InMemoryUserQueryRepository();
		this.alcoholQueryRepository = new InMemoryAlcoholQueryRepository();
	}

	@Nested
	@DisplayName("별점 등록")
	class RegisterRatingPoint {

		//@Test
		@DisplayName("신규 별점을 등록할 수 있다.")
		void test_1() {
			//given
			//when
			//then
		}

		//@Test
		@DisplayName("기존 별점을 수정 할 수 있다.")
		void test_2() {
			//given
			//when
			//then
		}

	}
}
