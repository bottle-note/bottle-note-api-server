package app.bottlenote.global.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients(basePackages = "app")
@Configuration
public class FeignConfig {}
