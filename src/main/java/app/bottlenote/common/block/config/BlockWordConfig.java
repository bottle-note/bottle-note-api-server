package app.bottlenote.common.block.config;

import app.bottlenote.common.block.serializer.BlockWordSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * BlockWord 시리얼라이저 설정
 */
@Configuration
@RequiredArgsConstructor
public class BlockWordConfig {

    private final ObjectMapper objectMapper;
    private final BlockWordSerializer blockWordSerializer;

    @PostConstruct
    public void configureObjectMapper() {
        SimpleModule blockWordModule = new SimpleModule("BlockWordModule");

        // String 타입에 대해 @BlockWord 어노테이션이 있을 때 커스텀 시리얼라이저 사용
        blockWordModule.addSerializer(String.class, blockWordSerializer);

        objectMapper.registerModule(blockWordModule);
    }
}
