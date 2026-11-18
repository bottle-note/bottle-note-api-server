package app.bottlenote.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {
  @Value("${amazon.aws.accessKey}")
  private String accessKeyId;

  @Value("${amazon.aws.secretKey}")
  private String accessKeySecret;

  @Value("${amazon.aws.region}")
  private String s3RegionName;

  /** s3 의 client 를 생성한다. */
  @Bean
  public S3Client getAmazonS3Client() {
    return S3Client.builder()
        .credentialsProvider(credentialsProvider())
        .region(Region.of(s3RegionName))
        .build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
        .credentialsProvider(credentialsProvider())
        .region(Region.of(s3RegionName))
        .build();
  }

  private StaticCredentialsProvider credentialsProvider() {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(accessKeyId, accessKeySecret));
  }
}
