package app.bottlenote.user.dto.response;

import app.bottlenote.user.dto.response.MyBottleResponse.BaseMyBottleInfo;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record RatingMyBottleItem(
    BaseMyBottleInfo baseMyBottleInfo,
    Double myRatingPoint,
    Double averageRatingPoint,
    Long averageRatingCount,
    LocalDateTime ratingModifyAt) {}
