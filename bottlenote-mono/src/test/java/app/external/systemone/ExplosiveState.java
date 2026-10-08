package app.external.systemone;

/** Jackson 직렬화 중 입력 값을 담은 예외를 던지는 state. 메시지 비노출 검증에 쓴다. */
public final class ExplosiveState {
  public static final String SECRET = "secret-value-boom";

  public String getValue() {
    throw new IllegalStateException(SECRET);
  }
}
