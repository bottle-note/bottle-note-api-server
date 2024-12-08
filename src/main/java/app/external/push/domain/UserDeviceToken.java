package app.external.push.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Builder
@Getter
@Table(name = "user_device_token",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"user_id", "device_token"})}
)
@Entity(name = "user_device_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDeviceToken {

	@Id
	@Comment("사용자 디바이스 토큰 id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("사용자 아이디")
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Comment("디바이스 토큰")
	@Column(name = "device_token", nullable = false)
	private String deviceToken;

	@Comment("플랫폼 (ANDROID, IOS)")
	@Enumerated(EnumType.STRING)
	@Column(name = "platform", nullable = false)
	private Platform platform;

	@Builder.Default
	@CreatedDate
	@Comment("생성일시")
	@Column(name = "create_at", nullable = false)
	private LocalDateTime createAt = LocalDateTime.now();

	@Builder.Default
	@LastModifiedDate
	@Comment("수정일시")
	@Column(name = "last_modify_at", nullable = false)
	private LocalDateTime lastModifyAt = LocalDateTime.now();

	public void updateModifiedAt() {
		this.lastModifyAt = LocalDateTime.now();
	}
}
