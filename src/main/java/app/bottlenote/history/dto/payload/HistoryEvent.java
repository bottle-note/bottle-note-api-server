package app.bottlenote.history.dto.payload;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

@Builder
public record HistoryEvent(
	Long userId,
	EventCategory eventCategory,
	EventType eventType,
	String redirectUrl,
	Long alcoholId,
	String message,
	Map<String, Object> dynamicMessage,
	String description
) {

	public HistoryEvent {
		validateUserId(userId);
		validateAlcoholId(alcoholId);
	}

	private void validateUserId(Long userId) {
		Objects.requireNonNull(userId, "userId must not be null");
	}

	private void validateAlcoholId(Long alcoholId) {
		Objects.requireNonNull(alcoholId, "alcoholId must not be null");
	}

	public static HistoryEvent makeHistoryEvent(
		Long userId,
		EventCategory eventCategory,
		EventType eventType,
		String redirectUrl,
		Long alcoholId,
		String message,
		Map<String, Object> dynamicMessage,
		String description
	) {
		return HistoryEvent.builder()
			.userId(userId)
			.eventCategory(eventCategory)
			.eventType(eventType)
			.redirectUrl(redirectUrl)
			.alcoholId(alcoholId)
			.message(message)
			.dynamicMessage(dynamicMessage)
			.description(description)
			.build();
	}
}
