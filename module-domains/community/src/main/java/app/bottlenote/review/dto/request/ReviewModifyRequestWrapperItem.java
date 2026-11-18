package app.bottlenote.review.dto.request;

import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.domain.ReviewTastingProfile;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

@Getter
public class ReviewModifyRequestWrapperItem {
  private final String content;
  private final ReviewDisplayStatus reviewDisplayStatus;
  private final BigDecimal price;
  private final SizeType sizeType;
  private final LocationInfoRequest locationInfo;
  private final Double rating;
  private final ReviewTastingProfile tastingProfile;

  public ReviewModifyRequestWrapperItem(
      app.bottlenote.review.dto.request.ReviewModifyRequest reviewModifyRequest) {
    this.content = reviewModifyRequest.content();
    this.reviewDisplayStatus = reviewModifyRequest.status();
    this.price = reviewModifyRequest.price();
    this.sizeType = reviewModifyRequest.sizeType();
    this.locationInfo =
        Objects.requireNonNullElse(reviewModifyRequest.locationInfo(), LocationInfoRequest.empty());
    this.rating = RatingPoint.of(reviewModifyRequest.rating()).getRating();
    this.tastingProfile = reviewModifyRequest.tastingProfile();
  }

  public static ReviewModifyRequestWrapperItem create(
      app.bottlenote.review.dto.request.ReviewModifyRequest reviewModifyRequest) {
    return new ReviewModifyRequestWrapperItem(reviewModifyRequest);
  }
}
