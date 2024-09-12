package app.bottlenote.support.help.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.domain.constant.HelpType;
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

import java.util.Objects;

@Comment("문의사항")
@Entity(name = "help")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Help extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("문의자")
	@Column(name = "userId", nullable = false)
	private Long userId;

	@Comment("문의 타입")
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private HelpType type;

	@Comment("문의 제목")
	@Column(name = "title", nullable = false)
	private String title;

	@Comment("문의 내용")
	@Column(name = "help_content", nullable = false)
	private String content;

	@Comment("문의글의 처리 상태")
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private StatusType status = StatusType.WAITING;

	@Comment("담당자( 추후 Admin으로 변경)")
	@Column(name = "admin_id", nullable = true)
	private Long adminId;

	@Comment("응답 내용")
	@Column(name = "response_content", nullable = false)
	private String responseContent;

	@Builder
	private Help(Long id, Long userId, HelpType type, String title, String content, Long adminId, String responseContent) {
		this.id = id;
		this.userId = userId;
		this.type = type;
		this.title = title;
		this.content = content;
		this.adminId = adminId;
		this.responseContent = responseContent;
	}

	public static Help create(Long userId, String title, String content, HelpType helpType) {
		return Help.builder()
			.userId(userId)
			.title(title)
			.content(content)
			.type(helpType)
			.build();
	}

	public void updateHelp(String title, String content, HelpType helpType){
		Objects.requireNonNull(title, "title은 필수입니다");
		Objects.requireNonNull(content, "content는 필수입니다");
		Objects.requireNonNull(helpType, "helpType은 필수입니다");
		this.title = title;
		this.content = content;
		this.type = helpType;
	}
}
