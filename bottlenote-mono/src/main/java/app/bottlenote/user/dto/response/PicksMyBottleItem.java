package app.bottlenote.user.dto.response;

import app.bottlenote.user.dto.response.MyBottleResponse.BaseMyBottleInfo;
import lombok.Builder;

@Builder
public record PicksMyBottleItem(
    BaseMyBottleInfo baseMyBottleInfo, boolean isPicked, Long totalPicksCount) {}
