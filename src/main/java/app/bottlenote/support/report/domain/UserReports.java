package app.bottlenote.support.report.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.report.constant.UserReportType;
import app.bottlenote.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Builder
@Getter
@Entity(name = "user_report")
@Table(name = "user_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserReports extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("신고자")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Comment("피신고자")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_user_id")
	private User reportUser;

	@Comment("유저 신고 타입")
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private UserReportType type;

	@Comment("신고내용")
	@Column(name = "report_content", nullable = false)
	private String content;

	@Comment("문의 처리결과의 답변내용")
	@Column(name = "response_content")
	private String responseContent;

	@Comment("문의글의 처리 상태 : Wating이 디폴트")
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private StatusType status = StatusType.WAITING;

	@Comment("해당 문의글을 처리한 관리자 아이디")
	@Column(name = "admin_id")
	private Long adminId;
}
