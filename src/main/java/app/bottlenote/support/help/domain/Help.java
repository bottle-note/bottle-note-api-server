package app.bottlenote.support.help.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Comment;

@Comment("문의사항")
@Entity(name = "help")
public class Help extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("문의자")
	@ManyToOne(fetch = FetchType.LAZY)
	private User user;

	@Comment("문의 타입") //todo : ENUM 클래스 만들어서 변경 필
	@Column(name = "type", nullable = false)
	private String type;

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

}
