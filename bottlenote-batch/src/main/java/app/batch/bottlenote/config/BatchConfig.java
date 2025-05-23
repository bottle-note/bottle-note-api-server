package app.batch.bottlenote.config;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.JobRegistrySmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
	private final JobRegistry jobRegistry;

	public BatchConfig(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	@Bean
	public JobRegistrySmartInitializingSingleton jobRegistrySmartInitializingSingleton() {
		return new JobRegistrySmartInitializingSingleton(this.jobRegistry);
	}
}
