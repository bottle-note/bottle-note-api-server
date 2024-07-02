package app.bottlenote.user.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Getter
@Table(name = "notification")
@Entity(name = "notification")
public class Notification extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("대상 사용자 식별자")
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Comment("알림 제목")
	@Column(name = "title", nullable = false)
	private String title;

	@Comment("알림 내용")
	@Column(name = "content", nullable = false)
	private String content;

	@Comment("알림 타입")
	@Column(name = "type", nullable = false)
	private String type;

	@Comment("알림 상태")
	@Column(name = "status", nullable = false)
	private String status;

	public Notification() {
	}
}
