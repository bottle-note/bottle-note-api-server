package app.bottlenote.history.dto.payload;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import java.util.Map;

public record HistoryEvent(
	Long userId,
	EventCategory eventCategory,
	EventType eventType,
	String redirectUrl,
	String imageUrl,
	Long alcoholId,
	String message,
	Map<String, Object> dynamicMessage,
	String description
) {
	public static HistoryEvent makeHistoryEvent(
		Long userId,
		EventCategory eventCategory,
		EventType eventType,
		String redirectUrl,
		String imageUrl,
		Long alcoholId,
		String message,
		Map<String, Object> dynamicMessage,
		String description
	) {
		return new HistoryEvent(
			userId,
			eventCategory,
			eventType,
			redirectUrl,
			imageUrl,
			alcoholId,
			message,
			dynamicMessage,
			description);
	}
}
