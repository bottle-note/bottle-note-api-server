package app.bottlenote.global.config;

import ch.qos.logback.core.rolling.RollingFileAppender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OSConditionalRollingFileAppender<E> extends RollingFileAppender<E> {

    @Override
    public void start() {
        String os = System.getProperty("os.name").toLowerCase();
        log.info("current os.name: {}", os);
        // 윈도우 환경 개발자는 없기 때문에, 윈도우 환경에서만 동작하는 로직은 X
        if (os.contains("mac")) {
            log.info("[Logback] Skipping JSON-FILE appender on macOS");
            return;
        }
        super.start();
    }
}
