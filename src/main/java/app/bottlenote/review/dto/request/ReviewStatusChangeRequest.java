package app.bottlenote.review.dto.request;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;

public record ReviewStatusChangeRequest(

	ReviewDisplayStatus status

) {

}
