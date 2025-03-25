package app.bottlenote.history.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.common.exception.CommonException;
import app.bottlenote.common.exception.CommonExceptionCode;
import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.EventType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Builder
@Getter
@Entity(name = "user_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHistory extends BaseEntity {

	@Id
	@Comment("이벤트 ID")
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("사용자 ID")
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Comment("이벤트 카테고리")
	@Enumerated(EnumType.STRING)
	@Column(name = "event_category", nullable = false)
	private EventCategory eventCategory;

	@Comment("이벤트 타입 카테고리의 세부 타입")
	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false)
	private EventType eventType;

	@Comment("클릭 시 리다이렉트 URL")
	@Column(name = "redirect_url")
	private String redirectUrl;

	@Comment("썸네일 URL")
	@Column(name = "image_url")
	private String imageUrl;

	@Comment("알콜 ID")
	@Column(name = "alcohol_id")
	private Long alcoholId;

	@Comment("이벤트 내용")
	@Column(name = "content")
	private String content;

	@Comment("동적 메시지")
	@Column(name = "dynamic_message", columnDefinition = "json")
	@Type(JsonType.class)
	private Map<String, String> dynamicMessage = new HashMap<>();

	@Comment("발생 년도")
	@Column(name = "event_year")
	private String eventYear;

	@Comment("발생 월")
	@Column(name = "event_month")
	private String eventMonth;

	public UserHistory(Long id, Long userId, EventCategory eventCategory, EventType eventType, String redirectUrl, String imageUrl, Long alcoholId, String content, Map<String, String> dynamicMessage,
					   String eventYear, String eventMonth) {
		this.id = id;
		this.userId = userId;
		this.eventCategory = eventCategory;
		this.eventType = eventType;
		this.redirectUrl = redirectUrl;
		this.imageUrl = imageUrl;
		this.alcoholId = alcoholId;
		this.content = content;
		this.dynamicMessage = dynamicMessage;
		this.eventYear = eventYear;
		this.eventMonth = eventMonth;
	}

	private String validateImageUrl(String imageUrl) {
		Objects.requireNonNull(imageUrl, "imageUrl must not be null");
		if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
			return imageUrl;
		}
		throw new CommonException(CommonExceptionCode.INVALID_IMAGE_URL);
	}
}
