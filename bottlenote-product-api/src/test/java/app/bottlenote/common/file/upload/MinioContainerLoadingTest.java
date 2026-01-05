package app.bottlenote.common.file.upload;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.operation.utils.TestContainersConfig;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.MinIOContainer;

@DisplayName("[integration] MinIO 컨테이너 로딩 테스트")
class MinioContainerLoadingTest extends IntegrationTestSupport {

  @Autowired private MinIOContainer minioContainer;

  @Autowired private AmazonS3 amazonS3;

  @Test
  @DisplayName("MinIO 컨테이너가 정상적으로 시작될 때 running 상태가 된다")
  void test_1() {
    // given & when
    boolean isRunning = minioContainer.isRunning();

    // then
    assertTrue(isRunning);
    log.info("MinIO 컨테이너 상태 = running: {}", isRunning);
    log.info("MinIO S3 URL = {}", minioContainer.getS3URL());
  }

  @Test
  @DisplayName("AmazonS3 클라이언트가 MinIO에 연결될 때 테스트 버킷이 존재한다")
  void test_2() {
    // given
    String testBucket = TestContainersConfig.getTestBucket();

    // when
    boolean bucketExists = amazonS3.doesBucketExistV2(testBucket);

    // then
    assertTrue(bucketExists);
    log.info("테스트 버킷 존재 여부 = {}: {}", testBucket, bucketExists);
  }

  @Test
  @DisplayName("AmazonS3 클라이언트가 정상적으로 주입될 때 null이 아니다")
  void test_3() {
    // given & when & then
    assertNotNull(amazonS3);
    log.info("AmazonS3 클라이언트 = {}", amazonS3);
  }
}
