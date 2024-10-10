package app.bottlenote.history.dto.payload;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;

import java.util.Map;

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
}
