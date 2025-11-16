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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@Profile({"test", "batch"})
@ActiveProfiles({"test", "batch"})
@Component
@SuppressWarnings("unchecked")
public class DataInitializer {
  private static final String OFF_FOREIGN_CONSTRAINTS = "SET foreign_key_checks = false";
  private static final String ON_FOREIGN_CONSTRAINTS = "SET foreign_key_checks = true";
  private static final String TRUNCATE_SQL_FORMAT = "TRUNCATE %s";
  private static final List<String> truncationDMLs = new ArrayList<>();

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
    log.info("데이터 초기화 시작");
    em.createNativeQuery(OFF_FOREIGN_CONSTRAINTS).executeUpdate();
    truncationDMLs.stream().map(em::createNativeQuery).forEach(Query::executeUpdate);
    em.createNativeQuery(ON_FOREIGN_CONSTRAINTS).executeUpdate();
    log.info("데이터 초기화 완료 - {}개 테이블 처리됨", truncationDMLs.size());
  }

  /** 캐시를 강제로 재초기화 후 전체 데이터 삭제 (테스트에서 동적 테이블 생성 시 사용) */
  @Transactional(value = REQUIRES_NEW)
  public void refreshCache() {
    log.info("데이터 초기화 시작 (캐시 재초기화)");
    synchronized (truncationDMLs) {
      truncationDMLs.clear();
      init();
      initialized = true;
    }
    em.createNativeQuery(OFF_FOREIGN_CONSTRAINTS).executeUpdate();
    truncationDMLs.stream().map(em::createNativeQuery).forEach(Query::executeUpdate);
    em.createNativeQuery(ON_FOREIGN_CONSTRAINTS).executeUpdate();
    log.info("데이터 초기화 완료 - {}개 테이블 처리됨", truncationDMLs.size());
  }

  private void initCache() {
    if (!initialized) {
      synchronized (truncationDMLs) {
        if (!initialized) {
          init();
          initialized = true;
        }
      }
    }
  }

  private void init() {
    final List<String> tableNames = em.createNativeQuery("SHOW TABLES ").getResultList();
    tableNames.stream()
        .filter(tableName -> !isSystemTable((String) tableName))
        .map(tableName -> String.format(TRUNCATE_SQL_FORMAT, tableName))
        .forEach(truncationDMLs::add);
  }

  private boolean isSystemTable(String tableName) {
    return SYSTEM_TABLE_PREFIXES.stream().anyMatch(prefix -> tableName.startsWith(prefix));
  }
}
