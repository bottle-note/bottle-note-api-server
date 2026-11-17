package app.bottlenote.user.dto.response;

import app.bottlenote.user.dto.response.MyBottleResponse.BaseMyBottleInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PicksMyBottleItem(
    BaseMyBottleInfo baseMyBottleInfo,
    boolean isPicked,
    Long totalPicksCount,
    @JsonIgnore LocalDateTime lastModifyAt,
    @JsonIgnore LocalDateTime lastReviewAt,
    @JsonIgnore Double myRatingPoint) {}
