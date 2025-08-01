package app.bottlenote.user.service;

import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NonceService {

	private static final String NONCE_PREFIX = "auth:nonce:";
	private static final Duration NONCE_TTL = Duration.ofMinutes(10);
	
	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * 클라이언트에게 전달할 일회성 Nonce 값을 생성하고 Redis에 저장
	 */
	@Transactional
	public String generateNonce() {
		String nonce = UUID.randomUUID().toString();
		String key = NONCE_PREFIX + nonce;
		
		redisTemplate.opsForValue().set(key, nonce, NONCE_TTL);
		log.debug("Nonce 생성 및 저장 완료: {}", nonce);
		
		return nonce;
	}

	/**
	 * 클라이언트로부터 받은 Nonce가 유효한지 검증
	 * 유효하다면 Redis에서 즉시 제거하여 재사용 방지
	 */
	@Transactional
	public void validateNonce(String nonce) {
		String key = NONCE_PREFIX + nonce;
		
		// getAndDelete를 사용하여 원자적으로 값을 가져오고 삭제
		Object stored = redisTemplate.opsForValue().getAndDelete(key);
		
		if (stored == null) {
			log.warn("유효하지 않은 Nonce 검증 시도: {}", nonce);
			throw new UserException(UserExceptionCode.INVALID_NONCE);
		}
		
		log.debug("Nonce 검증 및 제거 완료: {}", nonce);
	}
}
