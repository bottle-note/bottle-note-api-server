package app.bottlenote.global.config;

import app.bottlenote.common.block.serializer.BlockWordSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JacksonConfig {

  @Bean
  @Primary
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer(
      BlockWordSerializer blockWordSerializer) {
    log.info(
        "JacksonConfig Bean 생성 중 - BlockWordSerializer: {}",
        blockWordSerializer.getClass().getName());

    return builder -> {
      log.info("Jackson ObjectMapper 설정 적용 중");

      SimpleModule blockWordModule = new SimpleModule();

      // 모든 String 필드에 대해 BlockWordSerializer 적용
      // ContextualSerializer가 @BlockWord 어노테이션 확인
      blockWordModule.addSerializer(String.class, blockWordSerializer);
      log.info("BlockWordSerializer 모듈 등록 완료");

      builder.modules(new JavaTimeModule(), blockWordModule);
      builder.timeZone("Asia/Seoul"); // 기존 설정 유지
    };
  }
}
