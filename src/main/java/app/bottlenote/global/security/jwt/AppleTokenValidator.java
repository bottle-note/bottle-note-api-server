package app.bottlenote.global.security.jwt;

import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AppleTokenValidator implements TokenValidator {

	private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";
	private static final String APPLE_ISSUER = "https://appleid.apple.com";

	// application.yml 에서 실제 값 주입
	@Value("${apple.bundle-id}")
	private String appleAudience;

	// Apple 공개키는 자주 바뀌지 않으므로 캐싱하여 사용
	private final Cache<String, List<Map<String, String>>> publicKeyCache = CacheBuilder.newBuilder()
		.expireAfterWrite(1, TimeUnit.DAYS)
		.build();

	public Claims validateAndGetClaims(String idToken, String expectedNonce) {
		String headerOfIdToken = idToken.substring(0, idToken.indexOf('.'));
		Map<String, String> header;
		try {
			header = new ObjectMapper().readValue(new String(Base64.getDecoder().decode(headerOfIdToken)), Map.class);
		} catch (JsonProcessingException e) {
			throw new UserException(UserExceptionCode.APPLE_ID_TOKEN_HEADER_PARSING_ERROR);
		}

		PublicKey publicKey = getPublicKey(header);

		Claims claims;
		try {
			claims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.requireIssuer(APPLE_ISSUER)
				.requireAudience(appleAudience)
				.build()
				.parseClaimsJws(idToken)
				.getBody();
		} catch (ExpiredJwtException e) {
			throw new UserException(UserExceptionCode.APPLE_ID_TOKEN_EXPIRED);
		} catch (Exception e) {
			throw new UserException(UserExceptionCode.INVALID_APPLE_ID_TOKEN);
		}


		String nonceFromToken = claims.get("nonce", String.class);
		if (nonceFromToken == null || !nonceFromToken.equals(expectedNonce)) {
			throw new UserException(UserExceptionCode.NONCE_MISMATCH);
		}

		return claims;
	}

	private PublicKey getPublicKey(Map<String, String> header) {
		List<Map<String, String>> publicKeys = getApplePublicKeys();
		Map<String, String> matchedKey = publicKeys.stream()
			.filter(key -> key.get("kid").equals(header.get("kid")) && key.get("alg").equals(header.get("alg")))
			.findFirst()
			.orElseThrow(() -> new UserException(UserExceptionCode.NO_MATCHING_APPLE_PUBLIC_KEY));

		return createPublicKey(matchedKey);
	}

	private List<Map<String, String>> getApplePublicKeys() {
		return publicKeyCache.getIfPresent(APPLE_PUBLIC_KEYS_URL) != null ?
			publicKeyCache.getIfPresent(APPLE_PUBLIC_KEYS_URL) :
			fetchApplePublicKeys();
	}

	private List<Map<String, String>> fetchApplePublicKeys() {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, List<Map<String, String>>> keys = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, Map.class);
		publicKeyCache.put(APPLE_PUBLIC_KEYS_URL, keys.get("keys"));
		return keys.get("keys");
	}

	private PublicKey createPublicKey(Map<String, String> keyData) {
		try {
			BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(keyData.get("n")));
			BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(keyData.get("e")));
			RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePublic(publicKeySpec);
		} catch (Exception ex) {
			throw new UserException(UserExceptionCode.APPLE_PUBLIC_KEY_GENERATION_ERROR);
		}
	}

	public String getAppleSocialUniqueId(Claims claims) {
		return claims.getSubject();
	}

	public String getEmail(Claims claims) {
		return claims.get("email", String.class);
	}
}
