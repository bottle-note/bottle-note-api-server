package app.bottlenote.statistics.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StatisticsProperties.class)
public class StatisticsConfiguration {}
