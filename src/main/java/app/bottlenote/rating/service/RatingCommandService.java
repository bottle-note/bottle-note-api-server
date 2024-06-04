package app.bottlenote.rating.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.exception.RatingException;
import app.bottlenote.rating.exception.RatingExceptionCode;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingCommandService {

	private final RatingRepository ratingRepository;
	private final UserQueryRepository userQueryRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;

	public String register(Long alcoholId, Long userId, RatingPoint ratingPoint) {

		User user = userQueryRepository.findById(userId)
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		Alcohol alcohol = alcoholQueryRepository.findById(alcoholId)
			.orElseThrow(() -> new RatingException(RatingExceptionCode.ALCOHOL_NOT_FOUND));

		Rating rating = ratingRepository.findByAlcoholIdAndUserId(alcoholId, userId)
			.orElseGet(() -> Rating.builder()
				.id(RatingId.is(userId, alcoholId))
				.alcohol(alcohol)
				.user(user)
				.ratingPoint(ratingPoint)
				.build()
			);

		rating.registerRatingPoint(ratingPoint);

		Rating save = ratingRepository.save(rating);

		return save.toString();
	}
}
