package app.bottlenote.rating.dto.response;

import java.util.List;

public record RatingListFetchResponse(List<Info> ratings) {
  public static RatingListFetchResponse create(List<Info> ratingList) {
    return new RatingListFetchResponse(ratingList);
  }

  public record Info(
      Long alcoholId,
      String imageUrl,
      String korName,
      String engName,
      String korCategoryName,
      String engCategoryName,
      Boolean isPicked) {}
}
