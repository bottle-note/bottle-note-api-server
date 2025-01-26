package app.bottlenote.history.dto.response;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Getter
@NoArgsConstructor
@SuppressWarnings("unchecked")
public class UserHistoryDetail {

	private Long historyId;
	private LocalDateTime createdAt;
	private EventCategory eventCategory;
	private EventType eventType;
	private Long alcoholId;
	private String alcoholName;
	private String imageUrl;
	private String redirectUrl;
	private String description;
	private String message;
	private Map<String, String> dynamicMessage;

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
		this.dynamicMessage = (Map<String, String>) Optional.ofNullable(dynamicMessage)
			.filter(Map.class::isInstance).orElse(null);

	}
}
