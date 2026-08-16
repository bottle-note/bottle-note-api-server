package app.bottlenote.operation.utils;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 테스트 간 격리를 위해 스키마의 모든 테이블을 비운다.
 *
 * <p>TRUNCATE가 아니라 DELETE를 쓴다. InnoDB에서 TRUNCATE는 테이블스페이스를 drop/recreate하는 DDL이라 테이블당 약 8ms가 들고, 77개
 * 테이블 기준 한 사이클이 750ms를 넘는다. DELETE는 같은 조건에서 6ms 수준이다. 대신 AUTO_INCREMENT가 초기화되지 않으므로 생성 ID는 테스트 전체에서
 * 단조 증가한다.
 */
@Slf4j
@Component
@SuppressWarnings("unchecked")
public class DataInitializer {
  private static final String OFF_FOREIGN_CONSTRAINTS = "SET foreign_key_checks = false";
  private static final String ON_FOREIGN_CONSTRAINTS = "SET foreign_key_checks = true";
  private static final String DELETE_SQL_FORMAT = "DELETE FROM %s";
  private static final List<String> cleanupDMLs = new ArrayList<>();

  private static volatile boolean initialized = false;

  private static final Set<String> SYSTEM_TABLE_PREFIXES =
      Set.of("flyway_", "databasechangelog", "schema_version");

  @PersistenceContext private EntityManager em;

  protected DataInitializer() {}

  @Transactional(value = REQUIRES_NEW)
  public void deleteAll() {
    if (!initialized) {
      initCache();
    }
    log.debug("데이터 초기화 시작");
    executeCleanup();
  }

  /** 캐시를 강제로 재초기화 후 전체 데이터 삭제 (테스트에서 동적 테이블 생성 시 사용) */
  @Transactional(value = REQUIRES_NEW)
  public void refreshCache() {
    log.debug("데이터 초기화 시작 (캐시 재초기화)");
    synchronized (cleanupDMLs) {
      cleanupDMLs.clear();
      init();
      initialized = true;
    }
    executeCleanup();
    log.debug("데이터 초기화 완료 - {}개 테이블 처리됨", cleanupDMLs.size());
  }

  private void executeCleanup() {
    em.createNativeQuery(OFF_FOREIGN_CONSTRAINTS).executeUpdate();
    cleanupDMLs.stream().map(em::createNativeQuery).forEach(Query::executeUpdate);
    em.createNativeQuery(ON_FOREIGN_CONSTRAINTS).executeUpdate();
  }

  private void initCache() {
    if (!initialized) {
      synchronized (cleanupDMLs) {
        if (!initialized) {
          init();
          initialized = true;
        }
      }
    }
  }

  private void init() {
    final List<String> tableNames =
        em.createNativeQuery(
                "SELECT TABLE_NAME FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'")
            .getResultList();
    tableNames.stream()
        .filter(tableName -> !isSystemTable((String) tableName))
        .map(tableName -> String.format(DELETE_SQL_FORMAT, tableName))
        .forEach(cleanupDMLs::add);
  }

  private boolean isSystemTable(String tableName) {
    String lower = tableName.toLowerCase();
    return SYSTEM_TABLE_PREFIXES.stream().anyMatch(lower::startsWith);
  }
}
