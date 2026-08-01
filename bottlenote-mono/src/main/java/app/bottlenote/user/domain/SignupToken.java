package app.bottlenote.user.domain;

import app.bottlenote.user.constant.SocialType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity(name = "signup_tokens")
@Table(name = "signup_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignupToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "token_id", nullable = false, unique = true, length = 36)
  private UUID tokenId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "social_type", nullable = false, length = 20)
  private SocialType socialType;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "consumed_at")
  private LocalDateTime consumedAt;

  @Column(name = "create_at", nullable = false)
  private LocalDateTime createAt;

  public static SignupToken issue(
      UUID tokenId, Long userId, SocialType socialType, LocalDateTime expiresAt) {
    return new SignupToken(null, tokenId, userId, socialType, expiresAt, null, LocalDateTime.now());
  }

  public boolean belongsTo(Long userId, SocialType socialType) {
    return this.userId.equals(userId) && this.socialType == socialType;
  }

  public boolean isExpired(LocalDateTime now) {
    return !expiresAt.isAfter(now);
  }

  public boolean isConsumed() {
    return consumedAt != null;
  }

  public void consume(LocalDateTime now) {
    if (isConsumed()) {
      throw new IllegalStateException("이미 소진된 가입 토큰입니다.");
    }
    this.consumedAt = now;
  }
}
