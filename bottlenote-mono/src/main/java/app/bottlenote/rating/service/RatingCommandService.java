package app.bottlenote.rating.service;

import static java.lang.Boolean.FALSE;

import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.observability.service.TracingService;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.event.payload.RatingRegistryEvent;
import app.bottlenote.rating.exception.RatingException;
import app.bottlenote.rating.exception.RatingExceptionCode;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.UserFacade;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingCommandService {

  private final RatingRepository ratingRepository;
  private final UserFacade userFacade;
  private final AlcoholFacade alcoholFacade;
  private final HistoryEventPublisher ratingEventPublisher;
  private final Optional<TracingService> tracingService;

  @Transactional
  public RatingRegisterResponse register(Long alcoholId, Long userId, RatingPoint ratingPoint) {
    Objects.requireNonNull(alcoholId, "알코올 ID는 필수 값입니다.");
    Objects.requireNonNull(userId, "유저 ID는 필수 값입니다.");
    Objects.requireNonNull(ratingPoint, "별점은 필수 값입니다.");

    if (FALSE.equals(alcoholFacade.existsByAlcoholId(alcoholId))) {
      throw new RatingException(RatingExceptionCode.ALCOHOL_NOT_FOUND);
    }
    if (FALSE.equals(userFacade.existsByUserId(userId))) {
      throw new UserException(UserExceptionCode.USER_NOT_FOUND);
    }

    boolean isExistPrevRating = false;
    RatingPoint prevRatingPoint = null;

    // 기존 별점이 있는지 확인
    Rating rating = ratingRepository.findByAlcoholIdAndUserId(alcoholId, userId).orElse(null);

    if (rating == null) {
      rating =
          Rating.builder().id(new RatingId(userId, alcoholId)).ratingPoint(ratingPoint).build();
    } else {
      isExistPrevRating = true;
      prevRatingPoint = rating.getRatingPoint();
    }
    rating.registerRatingPoint(ratingPoint);
    Rating save = ratingRepository.save(rating);

    ratingEventPublisher.publishRatingHistoryEvent(
        RatingRegistryEvent.of(
            rating.getId().getAlcoholId(),
            rating.getId().getUserId(),
            isExistPrevRating ? prevRatingPoint : null,
            ratingPoint));

    // 평점 등록 이벤트 로깅
    String traceId = tracingService.map(TracingService::getCurrentTraceId).orElse("N/A");
    String action = isExistPrevRating ? "수정" : "등록";
    log.info(
        "평점 {} - userId: {}, alcoholId: {}, rating: {}, prevRating: {}, traceId: {}",
        action,
        userId,
        alcoholId,
        ratingPoint.getRating(),
        isExistPrevRating ? prevRatingPoint.getRating() : "N/A",
        traceId);

    return RatingRegisterResponse.success(save.getRatingPoint().getRating());
  }
}
