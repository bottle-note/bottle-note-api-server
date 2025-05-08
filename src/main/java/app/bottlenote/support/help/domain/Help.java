package app.bottlenote.support.help.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpImageItem;
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

@Comment("문의사항")
@Entity(name = "help")
@Table(name = "helps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Help extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("문의자")
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Comment("문의 타입")
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private HelpType type;

	@Comment("문의 내용")
	@Column(name = "help_content", nullable = false)
	private String content;

	@Embedded
	@Comment("문의글 이미지")
	private HelpImageList helpImageList = new HelpImageList();

	@Comment("문의글의 처리 상태")
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private StatusType status = StatusType.WAITING;

	@Comment("담당자( 추후 Admin으로 변경)")
	@Column(name = "admin_id")
	private Long adminId;

	@Comment("응답 내용")
	@Column(name = "response_content", nullable = false)
	private String responseContent;

	@Builder
	public Help(Long id, Long userId, HelpType type, String content, Long adminId, String responseContent) {
		this.id = id;
		this.userId = userId;
		this.type = type;
		this.content = content;
		this.adminId = adminId;
		this.responseContent = responseContent;
	}

	public static Help create(Long userId, HelpType helpType, String content) {
		return Help.builder()
			.userId(userId)
			.type(helpType)
			.content(content)
			.build();
	}

	public void saveImages(List<HelpImageItem> images, Long helpId) {
		helpImageList.addImages(images, helpId);
	}

	public void updateImages(List<HelpImageItem> images, Long helpId) {
		helpImageList.clear();
		helpImageList.addImages(images, helpId);
	}

	public void updateHelp(String content, List<HelpImageItem> images, HelpType helpType) {
		Objects.requireNonNull(content, "content는 필수입니다");
		Objects.requireNonNull(helpType, "helpType은 필수입니다");
		this.content = content;
		this.type = helpType;
		updateImages(images, this.id);
	}

	public void deleteHelp(){
		this.status = StatusType.DELETED;
	}

	public boolean isMyHelpPost(Long userId) {
		return this.userId.equals(userId);
	}
}
