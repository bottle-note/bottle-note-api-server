package app.bottlenote.agent.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
  @Column(name = "profile_code", nullable = false, length = 10)
  private String profileCode;

  @Comment("비밀 키(UUID) SHA-256 해시")
  @Column(name = "secret_hash", nullable = false, unique = true, columnDefinition = "char(64)")
  private String secretHash;

  @Builder.Default
  @Comment("활성화 여부")
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  public boolean isUsable() {
    return Boolean.TRUE.equals(isActive);
  }
}
