package app.bottlenote.security;

import java.security.Key;
import java.util.Date;

import app.bottlenote.oauth.constant.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class JwtTokenProvider {

    private final Key secretKey;
    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 15;
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;
    public static final String KEY_ROLES = "roles";


    public JwtTokenProvider(@Value("${security.jwt.secret-key}") String secret) {
		log.info("Secret key: {}", secret); // 이 부분을 추가
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // 엑세스 토큰 코드
    public JwtDto GenerateToken(String userEmail, UserType role){
        Claims claims = Jwts.claims().setSubject(userEmail);

        claims.put(KEY_ROLES,role);

        Date now = new Date();

        String accessToken = Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();


        String refreshToken = Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();

        return JwtDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();

    }

}
