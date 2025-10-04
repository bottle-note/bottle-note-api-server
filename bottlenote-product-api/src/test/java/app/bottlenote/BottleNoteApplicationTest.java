package app.bottlenote;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] [service] UserCommandService")
class BottleNoteApplicationTest extends IntegrationTestSupport {

  @Test
  @DisplayName("컨텍스트 로드 확인 테스트")
  void contextLoads() {
    log.info("MY_SQL_CONTAINER using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());
    log.info("REDIS_CONTAINER using port : {}", REDIS_CONTAINER.getFirstMappedPort());
    boolean running = MY_SQL_CONTAINER.isRunning();
    assertThat(running).isTrue();
    log.info("Context loaded successfully");
  }
}
