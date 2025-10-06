package app.bottlenote.user.fake;

import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.NonceService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FakeNonceService extends NonceService {

  private final Set<String> validNonces = new HashSet<>();

  public FakeNonceService() {
    super(null);
  }

  @Override
  public String generateNonce() {
    String nonce = "fake-nonce-" + UUID.randomUUID();
    validNonces.add(nonce);
    return nonce;
  }

  @Override
  public void validateNonce(String nonce) {
    if (!validNonces.contains(nonce)) {
      throw new UserException(UserExceptionCode.INVALID_NONCE);
    }
    // 사용 후 제거 (일회성)
    validNonces.remove(nonce);
  }

  // 테스트 유틸리티 메서드
  public void addValidNonce(String nonce) {
    validNonces.add(nonce);
  }

  public void clearNonces() {
    validNonces.clear();
  }
}
