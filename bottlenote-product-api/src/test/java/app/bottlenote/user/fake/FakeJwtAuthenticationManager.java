package app.bottlenote.user.fake;

import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class FakeJwtAuthenticationManager extends JwtAuthenticationManager {

  public FakeJwtAuthenticationManager() {
    // 더 긴 Base64 인코딩된 시크릿 키 (512비트)
    super(
        "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10ZXN0aW5nLXB1cnBvc2UtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTUxMi12ZXJzaW9uLWFuZC1pbXBvcnRhbnQtc2VjdXJpdHktaW4tbW9kZXJuLWphdmEtYXBwbGljYXRpb25z",
        null);
  }

  @Override
  public Authentication getAuthentication(String token) {
    // Fake 토큰에서 사용자 정보를 추출하는 로직
    String email = "test@example.com";
    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    return new UsernamePasswordAuthenticationToken(email, null, authorities);
  }
}
