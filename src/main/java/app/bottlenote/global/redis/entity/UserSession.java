package app.bottlenote.global.redis.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 세션 정보를 Redis에 저장하기 위한 엔티티 클래스. <p>
 * 로그인 세션 관리, 디바이스 추적 및 활성 사용자 관리에 사용됩니다. <p>
 * TTL(Time-To-Live)을 통해 세션의 자동 만료를 처리합니다.
 */
@Getter
@ToString
@RedisHash("user_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserSession implements Serializable {

	@Id
	@Comment("세션 ID")
	private String id;

	@Indexed
	@Comment("사용자 ID")
	private Long userId;

	@Comment("사용자 이름")
	private String username;

	@Comment("디바이스 정보")
	private String deviceInfo;

	@Comment("세션 시작 시간")
	private long loginTime;

	@Comment("세션 만료 시간")
	@TimeToLive(unit = TimeUnit.SECONDS)
	private long ttl;

	@Comment("로그인 리프레쉬 토큰 정보")
	private String refreshToken;

	public void refresh(String refreshToken, long ttl) {
		this.refreshToken = refreshToken;
		this.ttl = ttl;
	}


}
