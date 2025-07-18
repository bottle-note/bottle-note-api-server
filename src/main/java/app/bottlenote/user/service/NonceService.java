package app.bottlenote.user.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.transaction.annotation.Transactional;

@Service
public class NonceService {

	// Nonce를 10분 동안만 저장하는 캐시
	private final Cache<String, String> nonceStore = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();

	/**
	 * 클라이언트에게 전달할 일회성 Nonce 값을 생성하고 캐시에 저장
	 */
	@Transactional
	public String generateNonce() {
		String nonce = UUID.randomUUID().toString();
		nonceStore.put(nonce, nonce); // key-value 형태로 저장
		return nonce;
	}

	/**
	 * 클라이언트로부터 받은 Nonce가 유효한지 검증
	 * 유효하다면 캐시에서 즉시 제거하여 재사용 방지
	 */
	@Transactional
	public void validateNonce(String nonce) {
		if (nonceStore.getIfPresent(nonce) == null) {
			// 캐시에 nonce가 없으면 유효하지 않거나 만료된 것으로 간주
			throw new IllegalArgumentException("유효하지 않은 Nonce 값입니다.");
		}
		// 한번 사용한 Nonce는 즉시 무효화
		nonceStore.invalidate(nonce);
	}
}
