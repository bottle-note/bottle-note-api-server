package app.bottlenote.user.domain;

import app.bottlenote.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 사용자 차단 관계를 나타내는 엔티티
 */
@Getter
@Entity(name = "userBlock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
		name = "user_block",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_blocker_blocked",
						columnNames = {"blocker_id", "blocked_id"}
				)
		},
		indexes = {
				@Index(name = "idx_user_block_blocker", columnList = "blocker_id"),
				@Index(name = "idx_user_block_blocked", columnList = "blocked_id")
		}
)
public class UserBlock extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Comment("차단 관계 ID")
	private Long id;

	@Column(name = "blocker_id", nullable = false)
	@Comment("차단한 사용자 ID")
	private Long blockerId;

	@Column(name = "blocked_id", nullable = false)
	@Comment("차단당한 사용자 ID")
	private Long blockedId;

	@Column(name = "block_reason", length = 500)
	@Comment("차단 사유")
	private String blockReason;

	@Builder
	public UserBlock(Long blockerId, Long blockedId, String blockReason) {
		this.blockerId = blockerId;
		this.blockedId = blockedId;
		this.blockReason = blockReason;
	}

	public static UserBlock create(Long blockerId, Long blockedId, String reason) {
		return UserBlock.builder()
				.blockerId(blockerId)
				.blockedId(blockedId)
				.blockReason(reason)
				.build();
	}
}
