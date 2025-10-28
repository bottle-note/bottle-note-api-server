package app.bottlenote.observability.service;

import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * Tracing 서비스 인터페이스 Logger를 주입받아 사용하는 측의 logger로 로깅하며, management.tracing.enabled 설정에 따라 구현체가
 * 선택됩니다.
 */
public interface TracingService {

  /**
   * 새로운 스팬을 생성하고 실행합니다.
   *
   * @param logger 로깅에 사용할 Logger
   * @param spanName 스팬 이름
   * @param supplier 실행할 코드
   * @return 실행 결과
   */
  <T> T withSpan(Logger logger, String spanName, Supplier<T> supplier);

  /**
   * 새로운 스팬을 생성하고 태그를 추가한 후 실행합니다.
   *
   * @param logger 로깅에 사용할 Logger
   * @param spanName 스팬 이름
   * @param tags 추가할 태그들
   * @param supplier 실행할 코드
   * @return 실행 결과
   */
  <T> T withSpan(Logger logger, String spanName, Map<String, String> tags, Supplier<T> supplier);

  /**
   * 현재 스팬에 태그를 추가합니다.
   *
   * @param key 태그 키
   * @param value 태그 값
   */
  void addTag(String key, String value);

  /**
   * 현재 스팬에 여러 태그를 추가합니다.
   *
   * @param tags 추가할 태그들
   */
  void addTags(Map<String, String> tags);

  /**
   * 현재 스팬에 이벤트를 추가합니다.
   *
   * @param eventName 이벤트 이름
   */
  void addEvent(String eventName);

  /**
   * 현재 스팬에 이벤트와 태그를 추가합니다.
   *
   * @param eventName 이벤트 이름
   * @param tags 이벤트 태그들
   */
  void addEvent(String eventName, Map<String, String> tags);

  /**
   * 현재 트레이스 ID를 반환합니다.
   *
   * @return 트레이스 ID
   */
  String getCurrentTraceId();

  /**
   * 현재 스팬 ID를 반환합니다.
   *
   * @return 스팬 ID
   */
  String getCurrentSpanId();

  /**
   * 사용자 정보를 baggage에 추가합니다.
   *
   * @param userId 사용자 ID
   */
  void setUserId(String userId);

  /**
   * 테넌트 정보를 baggage에 추가합니다.
   *
   * @param tenantId 테넌트 ID
   */
  void setTenantId(String tenantId);

  /**
   * baggage에서 사용자 ID를 가져옵니다.
   *
   * @return 사용자 ID
   */
  String getUserId();

  /**
   * baggage에서 테넌트 ID를 가져옵니다.
   *
   * @return 테넌트 ID
   */
  String getTenantId();

  /**
   * 현재 스팬에 예외를 기록합니다.
   *
   * @param logger 로깅에 사용할 Logger
   * @param exception 예외
   */
  void recordException(Logger logger, Throwable exception);

  /**
   * 속성 추가 (OpenTelemetry 호환성)
   *
   * @param key 속성 키
   * @param value 속성 값
   */
  void addAttribute(String key, String value);

  /**
   * 속성 추가 (숫자형, OpenTelemetry 호환성)
   *
   * @param key 속성 키
   * @param value 속성 값
   */
  void addAttribute(String key, long value);

  /**
   * 속성 추가 (불린형, OpenTelemetry 호환성)
   *
   * @param key 속성 키
   * @param value 속성 값
   */
  void addAttribute(String key, boolean value);

  /**
   * 여러 속성 추가 (OpenTelemetry 호환성)
   *
   * @param attributes 추가할 속성들
   */
  void addAttributes(Map<String, String> attributes);
}