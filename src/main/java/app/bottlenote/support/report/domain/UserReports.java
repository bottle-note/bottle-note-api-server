package app.bottlenote.support.report.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.constant.StatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "user_reports")
public class UserReports extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reports_id")
	private Long id;

	@Comment("신고자의 문의내용")
	@Column(name = "user_content", nullable = false)
	private String userContent;

	@Comment("문의 처리결과의 답변내용")
	@Column(name = "response_content", nullable = false)
	private String responseContent;

	@Comment("문의글의 처리 상태 : Wating이 디폴트")
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private StatusType status = StatusType.WAITING;

	@Comment("해당 문의글을 처리한 관리자 아이디")
	@Column(name = "admin_id")
	private Long adminId;

}
