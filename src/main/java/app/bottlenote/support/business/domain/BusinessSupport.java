package app.bottlenote.support.business.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.business.constant.BusinessSupportType;
import app.bottlenote.support.business.dto.request.BusinessImageItem;
import app.bottlenote.support.constant.StatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

import java.util.List;
import java.util.Objects;

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

	@Comment("문의 제목")
	@Column(name = "title", nullable = false)
	private String title;

	@Comment("문의 내용")
	@Column(name = "content", nullable = false)
	private String content;

	@Comment("연락처")
	@Column(name = "contact", nullable = false)
	private String contact;

	@Comment("문의 유형")
	@Column(name = "business_support_type")
	@Enumerated(EnumType.STRING)
	private BusinessSupportType businessSupportType;

	@Embedded
	@Comment("비즈니스 문의 이미지")
	private BusinessImageList businessImageList = new BusinessImageList();

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	@Comment("문의 타입")
	private StatusType status = StatusType.WAITING;

	@Column(name = "admin_id")
	@Comment("어드민 ID")
	private Long adminId;

	@Column(name = "response_content")
	@Comment("문의 답변 내용")
	private String responseContent;

	@Builder
	private BusinessSupport(Long id, Long userId, String title, String content, String contact, BusinessSupportType businessSupportType, Long adminId, String responseContent) {
		this.id = id;
		this.userId = userId;
		this.title = title;
		this.content = content;
		this.contact = contact;
		this.businessSupportType = businessSupportType;
		this.adminId = adminId;
		this.responseContent = responseContent;
	}

	public static BusinessSupport create(Long userId, String title, String content, String contact, BusinessSupportType businessSupportType) {
		return BusinessSupport.builder()
				.userId(userId)
				.title(title)
				.content(content)
				.contact(contact)
				.businessSupportType(businessSupportType)
				.build();
	}

	public void saveImages(List<BusinessImageItem> images, Long businessSupportId) {
		businessImageList.addImages(images, businessSupportId);
	}

	public void updateImages(List<BusinessImageItem> images, Long businessSupportId) {
		businessImageList.clear();
		businessImageList.addImages(images, businessSupportId);
	}

	public void update(String title, String content, String contact, BusinessSupportType businessSupportType, List<BusinessImageItem> images) {
		Objects.requireNonNull(title, "title은 필수입니다");
		Objects.requireNonNull(content, "content는 필수입니다");
		Objects.requireNonNull(contact, "contact는 필수입니다");
		Objects.requireNonNull(businessSupportType, "contactType은 필수입니다");
		this.title = title;
		this.content = content;
		this.contact = contact;
		this.businessSupportType = businessSupportType;
		updateImages(images, this.id);
	}

	public void delete() {
		this.status = StatusType.DELETED;
	}

	public boolean isMyPost(Long userId) {
		return this.userId.equals(userId);
	}
}
