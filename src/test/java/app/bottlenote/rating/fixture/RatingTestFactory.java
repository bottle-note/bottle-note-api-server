package app.bottlenote.rating.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RatingTestFactory {

    @Autowired
    private RatingRepository ratingRepository;

    public void createRating(User user, Alcohol alcohol, int point) {
        Rating rating = Rating.builder()
                .id(RatingId.is(user.getId(), alcohol.getId()))
                .ratingPoint(RatingPoint.of(point))
                .build();
        ratingRepository.save(rating);
    }
}
