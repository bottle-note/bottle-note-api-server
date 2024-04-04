package app.bottlenote.support.report.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.user.domain.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Getter
@Entity
public class ReviewReport extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users user;

	@Comment("신고 사유")
	@Column(name = "report_content", nullable = false)
	private String report_content;

	@Comment("관리자 ID")
	@Column(name = "admin_id")
	private Long adminId;

	@Comment("처리 결과")
	@Column(name = "response_content", nullable = false)
	private String response_content;
}
