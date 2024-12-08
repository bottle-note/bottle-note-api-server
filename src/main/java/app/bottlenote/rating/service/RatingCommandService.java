package app.bottlenote.rating.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.payload.RatingRegistryEvent;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.event.publihser.RatingEventPublisher;
import app.bottlenote.rating.exception.RatingException;
import app.bottlenote.rating.exception.RatingExceptionCode;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingCommandService {

	private final RatingRepository ratingRepository;
	private final UserRepository userRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;
	private final RatingEventPublisher ratingEventPublisher;

	@Transactional
	public RatingRegisterResponse register(
		Long alcoholId,
		Long userId,
		RatingPoint ratingPoint
	) {
		Objects.requireNonNull(alcoholId, "알코올 ID는 필수 값입니다.");
		Objects.requireNonNull(userId, "유저 ID는 필수 값입니다.");
		Objects.requireNonNull(ratingPoint, "별점은 필수 값입니다.");

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		Alcohol alcohol = alcoholQueryRepository.findById(alcoholId)
			.orElseThrow(() -> new RatingException(RatingExceptionCode.ALCOHOL_NOT_FOUND));

		boolean isExistPrevRating = false;
		RatingPoint prevRatingPoint = null;

		// 기존 별점이 있는지 확인
		Rating rating = ratingRepository.findByAlcoholIdAndUserId(alcoholId, userId)
			.orElse(null);

		if (rating == null) {
			rating = Rating.builder()
				.id(RatingId.is(userId, alcoholId))
				.alcohol(alcohol)
				.user(user)
				.ratingPoint(ratingPoint)
				.build();
		} else {
			isExistPrevRating = true;
			prevRatingPoint = rating.getRatingPoint();
		}
		rating.registerRatingPoint(ratingPoint);
		Rating save = ratingRepository.save(rating);

		ratingEventPublisher.ratingRegistry(
			RatingRegistryEvent.of(
				rating.getId().getAlcoholId(),
				rating.getId().getUserId(),
				isExistPrevRating ? prevRatingPoint : null,
				ratingPoint
			)
		);

		return RatingRegisterResponse.success(save);
	}
}
