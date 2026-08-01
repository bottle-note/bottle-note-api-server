package app.bottlenote.agent.domain;

import app.bottlenote.agent.constant.AgentStatus;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Comment("에이전트 키 기반 인증 프로필")
@Entity(name = "agent")
@Table(name = "agents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Agent extends BaseEntity {

  @Id
  @Comment("에이전트 UUID")
  @Column(name = "id", columnDefinition = "char(36)")
  private String id;

  @Comment("프로필 코드 (0001~0006)")
  @Column(name = "profile_code", nullable = false, length = 4)
  private String profileCode;

  @Comment("에이전트 이름")
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Comment("에이전트 설명")
  @Column(name = "description", length = 500)
  private String description;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Comment("에이전트 상태")
  @Column(name = "status", nullable = false, length = 30)
  private AgentStatus status = AgentStatus.ACTIVE;

  @Comment("Product 사용자 ID")
  @Column(name = "product_user_id", nullable = false, unique = true)
  private Long productUserId;

  @Comment("Admin 사용자 ID")
  @Column(name = "admin_user_id", nullable = false, unique = true)
  private Long adminUserId;

  @Comment("API Key SHA-256 해시")
  @Column(name = "api_key_hash", nullable = false, unique = true, columnDefinition = "char(64)")
  private String apiKeyHash;

  @Builder.Default
  @Comment("API Key 발급 시각")
  @Column(name = "api_key_issued_at", nullable = false)
  private LocalDateTime apiKeyIssuedAt = LocalDateTime.now();

  @Comment("API Key 마지막 사용 시각")
  @Column(name = "last_used_at")
  private LocalDateTime lastUsedAt;

  public boolean isUsable() {
    return status == AgentStatus.ACTIVE;
  }
}
