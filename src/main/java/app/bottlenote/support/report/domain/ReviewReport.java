package app.bottlenote.support.report.domain;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "review_report")
public class ReviewReport extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Comment("신고 사유")
	@Column(name = "report_content", nullable = false)
	private String reportContent;

	//TODO : 광고 리뷰인지, 욕설 리뷰인지, Enum 클래스 정의 필요
	@Comment("리뷰 신고 타입")
	@Column(name = "type", nullable = false)
	private String type;

	@Comment("문의글의 처리 상태 : Wating이 디폴트")
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private StatusType status = StatusType.WAITING;

	@Comment("관리자 ID")
	@Column(name = "admin_id")
	private Long adminId;

	@Comment("처리 결과")
	@Column(name = "response_content", nullable = false)
	private String responseContent;
}
