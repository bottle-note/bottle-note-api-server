package app.bottlenote.global.config;

import app.bottlenote.common.block.serializer.BlockWordSerializer;
import app.bottlenote.shared.Const;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class JacksonConfig {

  @Bean
  @Primary
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer(
      BlockWordSerializer blockWordSerializer) {
    log.info("JacksonConfig Bean 생성 중");

    return builder -> {
      log.info("Jackson ObjectMapper 설정 적용 중");
      SimpleModule blockWordModule = new SimpleModule();
      blockWordModule.addSerializer(String.class, blockWordSerializer);
      builder.modules(new JavaTimeModule(), blockWordModule);
      builder.timeZone(Const.KOR_TIME_ZONE);
    };
  }
}
