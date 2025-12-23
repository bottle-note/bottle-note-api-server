package app.bottlenote.user.domain;

import app.bottlenote.common.domain.BaseTimeEntity;
import app.bottlenote.global.service.converter.AdminRoleConverter;
import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.constant.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Builder
@Getter
@Comment("관리자 사용자 테이블")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "admin_users")
@Table(name = "admin_users")
public class AdminUser extends BaseTimeEntity {

  @Id
  @Comment("관리자 ID")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("관리자 이메일 (로그인 ID)")
  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Comment("관리자 비밀번호")
  @Column(name = "password", nullable = false)
  private String password;

  @Comment("관리자 이름")
  @Column(name = "name", nullable = false)
  private String name;

  @Builder.Default
  @Convert(converter = AdminRoleConverter.class)
  @Comment("관리자 역할 목록")
  @Column(name = "roles", nullable = false, columnDefinition = "json")
  private List<AdminRole> roles = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Comment("관리자 상태")
  @Column(name = "status", nullable = false)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;

  @Comment("리프레시 토큰")
  @Column(name = "refresh_token", length = 512)
  private String refreshToken;

  @Comment("마지막 로그인 시간")
  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  public void updateRefreshToken(String refreshToken) {
    Objects.requireNonNull(refreshToken, "refreshToken은 null이 될 수 없습니다.");
    this.refreshToken = refreshToken;
  }

  public void updateLastLoginAt(LocalDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public void deactivate() {
    this.status = UserStatus.DELETED;
  }

  public boolean isActive() {
    return this.status == UserStatus.ACTIVE;
  }

  public boolean hasRole(AdminRole role) {
    return this.roles.contains(role);
  }

  public void addRole(AdminRole role) {
    if (!this.roles.contains(role)) {
      this.roles.add(role);
    }
  }

  public void removeRole(AdminRole role) {
    this.roles.remove(role);
  }
}
