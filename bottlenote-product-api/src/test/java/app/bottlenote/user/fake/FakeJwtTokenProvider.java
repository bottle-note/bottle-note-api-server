package app.bottlenote.user.fake;

import app.bottlenote.global.security.jwt.JwtTokenProvider;

public class FakeJwtTokenProvider extends JwtTokenProvider {

  public FakeJwtTokenProvider() {
    // 더 긴 Base64 인코딩된 시크릿 키 (512비트)
    super(
        "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10ZXN0aW5nLXB1cnBvc2UtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTUxMi12ZXJzaW9uLWFuZC1pbXBvcnRhbnQtc2VjdXJpdHktaW4tbW9kZXJuLWphdmEtYXBwbGljYXRpb25z");
  }

  // 실제 JWT 토큰을 생성하도록 부모 클래스의 메서드를 그대로 사용
  // 이렇게 하면 JwtTokenValidator.validateToken()에서 검증 가능한 실제 JWT가 생성됩니다
}
