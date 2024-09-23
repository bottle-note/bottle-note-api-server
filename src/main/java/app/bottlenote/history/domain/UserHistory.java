package app.bottlenote.history.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

@Getter
@Entity(name = "user_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_category", nullable = false)
	private EventCategory eventCategory;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false)
	private EventType eventType;

	@Column(name = "redirect_url")
	private String redirectUrl;

	@Column(name = "alcohol_id")
	private Long alcoholId;

	@Column(name = "message")
	private String message;

	@Column(name = "dynamic_message", columnDefinition = "json")
	@Type(JsonType.class)
	private Map<String, Object> dynamicMessage = new HashMap<>();

	@Column(name = "event_year")
	private String eventYear;

	@Column(name = "event_month")
	private String eventMonth;

	@Column(name = "description")
	private String description;
}
