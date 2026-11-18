package app.bottlenote.common.file.fixture;

import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.common.file.service.ResourceCommandService;
import app.bottlenote.operation.utils.TestContainersConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@TestConfiguration(proxyBeanMethods = false)
public class StorageTestConfig {
  @Bean("testImageUploadService")
  @Primary
  ImageUploadService imageUploadService(
      ResourceCommandService resourceCommandService,
      S3Presigner s3Presigner,
      @Value("${amazon.aws.cloudFrontUrl}") String cloudFrontUrl) {
    return new ImageUploadService(
        resourceCommandService, s3Presigner, TestContainersConfig.getTestBucket(), cloudFrontUrl);
  }
}
