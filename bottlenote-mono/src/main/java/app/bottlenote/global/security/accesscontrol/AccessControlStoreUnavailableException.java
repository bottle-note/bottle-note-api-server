package app.bottlenote.global.security.accesscontrol;

/** Access-control 저장소의 일시적 기술 장애를 구분한다. */
public class AccessControlStoreUnavailableException extends RuntimeException {

  public AccessControlStoreUnavailableException(Throwable cause) {
    super("access-control store is unavailable", cause);
  }
}
