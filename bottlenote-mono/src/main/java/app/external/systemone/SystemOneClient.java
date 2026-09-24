package app.external.systemone;

import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneResult;

/**
 * System One 계열 결정 모델 호출 계약. 공급자(Jev 등)를 교체해도 호출 측은 이 인터페이스만 의존한다.
 *
 * <p>공급자 오류는 예외 대신 {@link SystemOneResult.Failure}로 반환한다.
 */
public interface SystemOneClient {

  SystemOneResult evaluate(SystemOneRequest request);
}
