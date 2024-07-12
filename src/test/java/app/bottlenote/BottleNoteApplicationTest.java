package app.bottlenote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Integration] BottleNote 애플리케이션 테스트")
class BottleNoteApplicationTest extends IntegrationTestSupport {

	@Test
	@DisplayName("컨텍스트 로드 확인 테스트")
	public void contextLoads() {
		log.info("using port : {}", MY_SQL_CONTAINER.getFirstMappedPort());
		boolean running = MY_SQL_CONTAINER.isRunning();
		assertThat(running).isTrue();
		log.info("Context loaded successfully");
	}
}
