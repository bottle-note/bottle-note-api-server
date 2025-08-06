package app.bottlenote.user.fake;

import app.bottlenote.global.security.jwt.AppleTokenValidator;
import app.bottlenote.global.security.jwt.TokenValidator;
import io.jsonwebtoken.Claims;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FakeAppleTokenValidator extends AppleTokenValidator implements TokenValidator {

	@Override
	public Claims validateAndGetClaims(String idToken, String expectedNonce) {
		return new FakeClaims(expectedNonce);
	}
	
	@Override
	public String getSocialUniqueId(Claims claims) {
		return claims.getSubject();
	}

	@Override
	public String getAppleSocialUniqueId(Claims claims) {
		return getSocialUniqueId(claims);
	}
	
	@Override
	public String getEmail(Claims claims) {
		return claims.get("email", String.class);
	}

	private static class FakeClaims implements Claims {
		private final Map<String, Object> claims = new HashMap<>();

		public FakeClaims(String nonce) {
			claims.put("sub", "fake-apple-user-id");
			claims.put("email", "apple.user@example.com");
			claims.put("nonce", nonce);
			claims.put("iss", "https://appleid.apple.com");
			claims.put("aud", "com.bottlenote.official.app");
			claims.put("exp", new Date(System.currentTimeMillis() + 3600000));
			claims.put("iat", new Date());
		}

		@Override
		public String getIssuer() {
			return (String) claims.get("iss");
		}

		@Override
		public Claims setIssuer(String iss) {
			claims.put("iss", iss);
			return this;
		}

		@Override
		public String getSubject() {
			return (String) claims.get("sub");
		}

		@Override
		public Claims setSubject(String sub) {
			claims.put("sub", sub);
			return this;
		}

		@Override
		public String getAudience() {
			return (String) claims.get("aud");
		}

		@Override
		public Claims setAudience(String aud) {
			claims.put("aud", aud);
			return this;
		}

		@Override
		public Date getExpiration() {
			return (Date) claims.get("exp");
		}

		@Override
		public Claims setExpiration(Date exp) {
			claims.put("exp", exp);
			return this;
		}

		@Override
		public Date getNotBefore() {
			return (Date) claims.get("nbf");
		}

		@Override
		public Claims setNotBefore(Date nbf) {
			claims.put("nbf", nbf);
			return this;
		}

		@Override
		public Date getIssuedAt() {
			return (Date) claims.get("iat");
		}

		@Override
		public Claims setIssuedAt(Date iat) {
			claims.put("iat", iat);
			return this;
		}

		@Override
		public String getId() {
			return (String) claims.get("jti");
		}

		@Override
		public Claims setId(String jti) {
			claims.put("jti", jti);
			return this;
		}

		@Override
		public <T> T get(String claimName, Class<T> requiredType) {
			Object value = claims.get(claimName);
			if (value != null && requiredType.isAssignableFrom(value.getClass())) {
				return requiredType.cast(value);
			}
			return null;
		}

		@Override
		public int size() {
			return claims.size();
		}

		@Override
		public boolean isEmpty() {
			return claims.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return claims.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return claims.containsValue(value);
		}

		@Override
		public Object get(Object key) {
			return claims.get(key);
		}

		@Override
		public Object put(String key, Object value) {
			return claims.put(key, value);
		}

		@Override
		public Object remove(Object key) {
			return claims.remove(key);
		}

		@Override
		public void putAll(Map<? extends String, ?> m) {
			claims.putAll(m);
		}

		@Override
		public void clear() {
			claims.clear();
		}

		@Override
		public Set<String> keySet() {
			return claims.keySet();
		}

		@Override
		public Collection<Object> values() {
			return claims.values();
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			return claims.entrySet();
		}
	}
}
