package app.bottlenote.support.business.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.business.constant.ContactType;
import app.bottlenote.support.constant.StatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "business_support")
@Table(name = "business_supports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessSupport extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("문의자")
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Comment("문의 내용")
	@Column(name = "content", nullable = false)
	private String content;

	@Comment("연락 방식")
	@Column(name = "contact_way")
	@Enumerated(EnumType.STRING)
	private ContactType contactWay;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private StatusType status = StatusType.WAITING;

	@Column(name = "admin_id")
	private Long adminId;

	@Column(name = "response_content")
	private String responseContent;

	@Builder
	private BusinessSupport(Long id, Long userId, String content, ContactType contactWay, Long adminId, String responseContent) {
		this.id = id;
		this.userId = userId;
		this.content = content;
		this.contactWay = contactWay;
		this.adminId = adminId;
		this.responseContent = responseContent;
	}

	public static BusinessSupport create(Long userId, String content, ContactType contactWay) {
		return BusinessSupport.builder()
				.userId(userId)
				.content(content)
				.contactWay(contactWay)
				.build();
	}

	public void update(String content, ContactType contactWay) {
		this.content = content;
		this.contactWay = contactWay;
	}

	public void delete() {
		this.status = StatusType.DELETED;
	}

	public boolean isMyPost(Long userId) {
		return this.userId.equals(userId);
	}
}
