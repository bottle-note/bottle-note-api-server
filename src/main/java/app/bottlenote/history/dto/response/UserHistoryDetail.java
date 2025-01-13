package app.bottlenote.history.dto.response;

import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record UserHistoryDetail(
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

}
