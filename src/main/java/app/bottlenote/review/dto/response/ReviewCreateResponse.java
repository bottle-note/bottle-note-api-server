package app.bottlenote.review.dto.response;

import lombok.Getter;

@Getter
public class ReviewCreateResponse {

	private final Long id;

	public ReviewCreateResponse(Long id) {
		this.id = id;
	}
}
