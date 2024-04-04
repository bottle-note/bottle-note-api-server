package app.bottlenote.support.help.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.report.domain.StatusType;
import app.bottlenote.user.domain.Users;
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
	/*CREATE TABLE `문의` (
	`user_id`	VARCHAR(255)	NOT NULL	COMMENT '문의자',
	`help_content`	VARCHAR(255)	NOT NULL	COMMENT '어던 문제를 문의했는지.',
	`status`	VARCHAR(255)	NOT NULL	DEFAULT waiting	COMMENT '진행상태',
	`admin_id`	VARCHAR(255)	NULL	COMMENT '처리  어드민',
	`response_content`	VARCHAR(255)	NULL	COMMENT '처리 결과',
);*/

	@Comment("문의자")
	@ManyToOne(fetch = FetchType.LAZY)
	private Users user;

	@Comment("문의 제목")
	@Column(name = "title", nullable = false)
	private String title;

	@Comment("문의 내용")
	@Column(name = "content", nullable = false)
	private String content;

	@Comment("담당자( 추후 Admin으로 변경)")
	@Column(name = "admin_id", nullable = true)
	private Long adminId;

	@Comment("문의글의 처리 상태")
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private StatusType status = StatusType.WAITING;
}
