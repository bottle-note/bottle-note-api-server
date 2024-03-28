package main.java.app.bottlenote.security;

import java.util.Base64;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key secretKey;
    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 15;
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;
    public static final String KEY_ROLES = "roles";


    public JwtTokenProvider(@Value("${security.jwt.secret-key}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // 엑세스 토큰 코드
    private String GenerateToken(String userEmail, String role){
        Claims claims = Jwts.claims().setSubject(userEmail);

        claims.put(KEY_ROLES,role);

        Date now = new Date();

        String accessToken = Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + EXPIRE_TIME))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();


        String refreshToken = Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + REFRESH_EXPIRE_TIME))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();

        return JwtDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();

    }


}
