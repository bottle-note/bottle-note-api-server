package app.bottlenote.history.dto.response;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

// DTO 수정 예시
@Getter
public class UserHistoryDetail {
	
	private final Long historyId;
	private final LocalDateTime createdAt;
	private final EventCategory eventCategory;
	private final EventType eventType;
	private final Long alcoholId;
	private final String alcoholName;
	private final String imageUrl;
	private final String redirectUrl;
	private final String description;
	private final String message;
	private final Map<String, String> dynamicMessage;

	@Builder
	public UserHistoryDetail(
		Long historyId,
		LocalDateTime createdAt,
		EventCategory eventCategory,
		EventType eventType,
		Long alcoholId,
		String alcoholName,
		String imageUrl,
		String redirectUrl,
		String description,
		String message,
		Object dynamicMessage
	) {
		this.historyId = historyId;
		this.createdAt = createdAt;
		this.eventCategory = eventCategory;
		this.eventType = eventType;
		this.alcoholId = alcoholId;
		this.alcoholName = alcoholName;
		this.imageUrl = imageUrl;
		this.redirectUrl = redirectUrl;
		this.description = description;
		this.message = message;
		this.dynamicMessage = (Map<String, String>) dynamicMessage;
	}
}