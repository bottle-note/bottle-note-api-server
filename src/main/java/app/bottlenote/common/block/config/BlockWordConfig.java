package app.bottlenote.common.block.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * BlockWord 시리얼라이저 설정
 *
 * @BlockWord 어노테이션에 @JsonSerialize가 직접 포함되어 있으므로
 * 글로벌 시리얼라이저 등록은 불필요함
 */
@Slf4j
@Configuration
public class BlockWordConfig {

    public BlockWordConfig() {
        log.debug("BlockWordConfig 초기화 완료 - @BlockWord 어노테이션 자체에 @JsonSerialize 포함");
    }
}
