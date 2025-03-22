package app.bottlenote.common.profanity.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ProfanityResponse(
	String trackingId,
	Status status,
	List<Detected> detected,
	String filtered,
	String elapsed
) {
	public Boolean isProfane() {
		return detected != null && !detected.isEmpty();
	}

	public Boolean isNotFiltered() {
		return filtered == null || filtered.isEmpty();
	}

	public record Status(
		Integer code,
		String message,
		String description,
		String detailDescription
	) {
	}
}
