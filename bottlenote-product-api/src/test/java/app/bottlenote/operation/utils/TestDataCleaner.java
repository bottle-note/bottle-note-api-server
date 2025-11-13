package app.bottlenote.operation.utils;

import app.bottlenote.DataInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 테스트 데이터 정리를 담당하는 컴포넌트
 *
 * <p>DataInitializer를 래핑하여 데이터 정리 전략을 관리합니다.
 */
@Slf4j
@Component
public class TestDataCleaner {

  private final DataInitializer dataInitializer;

  public TestDataCleaner(DataInitializer dataInitializer) {
    this.dataInitializer = dataInitializer;
  }

  /** 전체 데이터 삭제 (모든 테이블 TRUNCATE) */
  public void cleanAll() {
    log.info("데이터 초기화 시작");
    dataInitializer.deleteAll();
    log.info("데이터 초기화 완료");
  }
}
