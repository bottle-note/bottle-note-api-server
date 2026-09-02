package app.bottlenote.alcohols.dto.response;

import app.bottlenote.mfds.dto.response.MfdsPublicDeclarationItem;
import app.bottlenote.review.dto.response.ReviewListResponse;
import java.util.List;
import lombok.Builder;

@Builder
public record AlcoholDetailResponse(
    AlcoholDetailItem alcohols,
    FriendsDetailResponse friendsInfo,
    ReviewListResponse reviewInfo,
    List<MfdsPublicDeclarationItem> mfdsDeclarations) {

  public AlcoholDetailResponse {
    mfdsDeclarations = mfdsDeclarations == null ? List.of() : List.copyOf(mfdsDeclarations);
  }
}
