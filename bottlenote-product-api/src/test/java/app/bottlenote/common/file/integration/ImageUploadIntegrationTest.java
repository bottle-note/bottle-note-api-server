package app.bottlenote.common.file.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] ImageUpload")
class ImageUploadIntegrationTest extends IntegrationTestSupport {

  @Autowired private ResourceLogRepository resourceLogRepository;

  @Nested
  @DisplayName("PreSigned URL 생성 테스트")
  class PreSignedUrlTest {

    @Test
    @DisplayName("인증된 사용자가 PreSigned URL 생성에 성공한다")
    void test_1() throws Exception {
      // given
      String rootPath = "review";
      Long uploadSize = 2L;

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/s3/presign-url")
              .param("rootPath", rootPath)
              .param("uploadSize", String.valueOf(uploadSize))
              .header("Authorization", "Bearer " + getToken())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      ImageUploadResponse response = extractData(result, ImageUploadResponse.class);
      assertNotNull(response);
      assertEquals(uploadSize.intValue(), response.uploadSize());
      assertEquals(uploadSize.intValue(), response.imageUploadInfo().size());
      assertNotNull(response.bucketName());
      assertEquals(5, response.expiryTime());

      response
          .imageUploadInfo()
          .forEach(
              item -> {
                assertNotNull(item.uploadUrl());
                assertNotNull(item.viewUrl());
                assertTrue(item.uploadUrl().contains("s3"));
              });

      log.info("PreSigned URL 생성 응답: {}", response);
    }

    @Test
    @DisplayName("여러 개의 이미지 업로드 URL을 한번에 생성할 수 있다")
    void test_2() throws Exception {
      // given
      String rootPath = "review";
      Long uploadSize = 3L;

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/s3/presign-url")
              .param("rootPath", rootPath)
              .param("uploadSize", String.valueOf(uploadSize))
              .header("Authorization", "Bearer " + getToken())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      ImageUploadResponse response = extractData(result, ImageUploadResponse.class);
      assertEquals(uploadSize.intValue(), response.imageUploadInfo().size());

      for (int i = 0; i < uploadSize; i++) {
        assertEquals(i + 1, response.imageUploadInfo().get(i).order());
        assertTrue(response.imageUploadInfo().get(i).viewUrl().contains(rootPath));
      }

      log.info("생성된 업로드 URL 수: {}", response.imageUploadInfo().size());
    }

    @Test
    @DisplayName("uploadSize 없이 요청하면 기본값 1로 처리된다")
    void test_3() throws Exception {
      // given
      String rootPath = "review";

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/s3/presign-url")
              .param("rootPath", rootPath)
              .header("Authorization", "Bearer " + getToken())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      ImageUploadResponse response = extractData(result, ImageUploadResponse.class);
      assertEquals(1, response.uploadSize());
      assertEquals(1, response.imageUploadInfo().size());
    }
  }

  @Nested
  @DisplayName("ResourceLog 저장 테스트")
  class ResourceLogTest {

    @Test
    @DisplayName("인증된 사용자가 PreSigned URL 생성 시 ResourceLog에 CREATED 이벤트가 저장된다")
    void test_1() throws Exception {
      // given
      String rootPath = "review";
      Long uploadSize = 2L;
      String token = getToken();
      Long userId = getTokenUserId();

      // when
      mockMvcTester
          .get()
          .uri("/api/v1/s3/presign-url")
          .param("rootPath", rootPath)
          .param("uploadSize", String.valueOf(uploadSize))
          .header("Authorization", "Bearer " + token)
          .contentType(APPLICATION_JSON)
          .with(csrf())
          .exchange();

      // then - 비동기 로그 저장 대기
      Awaitility.await()
          .atMost(3, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                assertFalse(logs.isEmpty());
                assertEquals(uploadSize.intValue(), logs.size());
              });

      List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
      logs.forEach(
          resourceLog -> {
            assertEquals(ResourceEventType.CREATED, resourceLog.getEventType());
            assertEquals("IMAGE", resourceLog.getResourceType());
            assertEquals(userId, resourceLog.getUserId());
            assertTrue(resourceLog.getResourceKey().startsWith(rootPath));
            assertNotNull(resourceLog.getViewUrl());
          });

      log.info("저장된 ResourceLog 수: {}", logs.size());
    }
  }
}
