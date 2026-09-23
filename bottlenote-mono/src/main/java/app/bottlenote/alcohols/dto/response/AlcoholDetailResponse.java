package app.bottlenote.alcohols.dto.response;

import app.bottlenote.review.dto.response.ReviewListResponse;
import lombok.Builder;

@Builder
public record AlcoholDetailResponse(
    ProductAlcoholDetailItem alcohols,
    FriendsDetailResponse friendsInfo,
    ReviewListResponse reviewInfo) {}
