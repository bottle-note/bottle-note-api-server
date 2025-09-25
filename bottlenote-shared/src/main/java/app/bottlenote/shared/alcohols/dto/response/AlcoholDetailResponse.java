package app.bottlenote.shared.alcohols.dto.response;

import app.bottlenote.shared.review.dto.response.ReviewListResponse;
import lombok.Builder;

@Builder
public record AlcoholDetailResponse(
    AlcoholDetailItem alcohols, FriendsDetailResponse friendsInfo, ReviewListResponse reviewInfo) {}
