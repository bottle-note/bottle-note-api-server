package app.bottlenote.global.redis.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 사용자의 주류 조회 기록을 Redis에 저장하기 위한 엔티티 클래스.
 * 사용자별 최근 조회 주류를 추적하고 관리하며, 일정 기간 후 자동 만료됩니다.
 */
@Getter
@ToString
@RedisHash("alcohol_view_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AlcoholViewHistory implements Serializable {

	@Id
	private UUID id;

	@Indexed
	private Long userId;

	@Indexed
	private Long alcoholId;

	private String alcoholName;

	private long viewTime;

	@TimeToLive(unit = TimeUnit.DAYS)
	private long ttl;
}
