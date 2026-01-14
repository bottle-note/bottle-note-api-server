package app.bottlenote.common.file.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import app.bottlenote.common.file.dto.response.ImageUploadItem;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfoRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfoRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import java.math.BigDecimal;
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
  @Autowired private AlcoholTestFactory alcoholTestFactory;

  private LocationInfoRequest createTestLocationInfo() {
    return new LocationInfoRequest(
        "테스트 장소", "12345", "서울시 강남구", "상세 주소", "BAR", "https://map.test.com", "37.123", "127.456");
  }

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

  @Nested
  @DisplayName("이미지 리소스 활성화 테스트")
  class ResourceActivationTest {

    @Test
    @DisplayName("리뷰 생성 시 이미지가 포함되면 ResourceLog 상태가 ACTIVATED로 변경된다")
    void test_review_with_images_creates_activated_log() throws Exception {
      // given
      String token = getToken();
      Long userId = getTokenUserId();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      // PreSigned URL 생성 (CREATED 로그 저장)
      MvcTestResult presignResult =
          mockMvcTester
              .get()
              .uri("/api/v1/s3/presign-url")
              .param("rootPath", "review")
              .param("uploadSize", "2")
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      ImageUploadResponse uploadResponse = extractData(presignResult, ImageUploadResponse.class);
      List<ImageUploadItem> uploadInfos = uploadResponse.imageUploadInfo();

      // CREATED 로그 저장 대기
      Awaitility.await()
          .atMost(3, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                assertEquals(2, logs.size());
              });

      // 리뷰 생성 요청 (이미지 URL 포함)
      List<ReviewImageInfoRequest> imageRequests =
          List.of(
              new ReviewImageInfoRequest(1L, uploadInfos.get(0).viewUrl()),
              new ReviewImageInfoRequest(2L, uploadInfos.get(1).viewUrl()));

      ReviewCreateRequest reviewRequest =
          new ReviewCreateRequest(
              alcohol.getId(),
              ReviewDisplayStatus.PUBLIC,
              "테스트 리뷰 내용입니다.",
              SizeType.GLASS,
              BigDecimal.valueOf(30000),
              createTestLocationInfo(),
              imageRequests,
              List.of("테이스팅태그"),
              4.5);

      // when
      MvcTestResult reviewResult =
          mockMvcTester
              .post()
              .uri("/api/v1/reviews")
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(reviewRequest))
              .with(csrf())
              .exchange();

      ReviewCreateResponse reviewResponse = extractData(reviewResult, ReviewCreateResponse.class);
      assertNotNull(reviewResponse.getId());

      // then - ACTIVATED 상태로 변경 대기 (Single Record 방식)
      Awaitility.await()
          .atMost(5, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                long activatedCount =
                    logs.stream()
                        .filter(log -> log.getEventType() == ResourceEventType.ACTIVATED)
                        .count();
                assertEquals(2, activatedCount);
              });

      // ACTIVATED 로그 검증 (총 레코드 수는 2개로 유지)
      List<ResourceLog> allLogs = resourceLogRepository.findByUserId(userId);
      assertEquals(2, allLogs.size());

      allLogs.forEach(
          activatedLog -> {
            assertEquals(ResourceEventType.ACTIVATED, activatedLog.getEventType());
            assertEquals(reviewResponse.getId(), activatedLog.getReferenceId());
            assertEquals("REVIEW", activatedLog.getReferenceType());
            assertTrue(activatedLog.getResourceKey().startsWith("review/"));
          });

      log.info("총 로그 수: {}, 모두 ACTIVATED 상태", allLogs.size());
    }

    @Test
    @DisplayName("이미지 없이 리뷰를 생성할 때 ACTIVATED 이벤트가 발생하지 않는다")
    void test_review_without_images_does_not_create_activated_log() throws Exception {
      // given
      String token = getToken();
      Long userId = getTokenUserId();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      ReviewCreateRequest reviewRequest =
          new ReviewCreateRequest(
              alcohol.getId(),
              ReviewDisplayStatus.PUBLIC,
              "이미지 없는 테스트 리뷰",
              SizeType.BOTTLE,
              BigDecimal.valueOf(50000),
              createTestLocationInfo(),
              List.of(),
              List.of(),
              3.0);

      // when
      MvcTestResult reviewResult =
          mockMvcTester
              .post()
              .uri("/api/v1/reviews")
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(reviewRequest))
              .with(csrf())
              .exchange();

      ReviewCreateResponse reviewResponse = extractData(reviewResult, ReviewCreateResponse.class);
      assertNotNull(reviewResponse.getId());

      // then - 잠시 대기 후 ACTIVATED 로그가 없는지 확인
      Thread.sleep(1000);
      List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
      long activatedCount =
          logs.stream().filter(log -> log.getEventType() == ResourceEventType.ACTIVATED).count();

      assertEquals(0, activatedCount);
      log.info("이미지 없는 리뷰 생성 후 ACTIVATED 로그 수: {}", activatedCount);
    }

    @Test
    @DisplayName("PreSigned URL 생성부터 리뷰 생성까지 전체 흐름에서 단일 레코드의 상태가 CREATED에서 ACTIVATED로 변경된다")
    void test_full_flow_created_to_activated() throws Exception {
      // given
      String token = getToken();
      Long userId = getTokenUserId();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      // 1. PreSigned URL 생성 -> CREATED 로그
      MvcTestResult presignResult =
          mockMvcTester
              .get()
              .uri("/api/v1/s3/presign-url")
              .param("rootPath", "review")
              .param("uploadSize", "1")
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      ImageUploadResponse uploadResponse = extractData(presignResult, ImageUploadResponse.class);
      String viewUrl = uploadResponse.imageUploadInfo().get(0).viewUrl();

      // CREATED 로그 대기
      Awaitility.await()
          .atMost(3, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                assertEquals(1, logs.size());
                assertEquals(ResourceEventType.CREATED, logs.get(0).getEventType());
              });

      // 2. 리뷰 생성 -> ACTIVATED 상태로 변경
      ReviewCreateRequest reviewRequest =
          new ReviewCreateRequest(
              alcohol.getId(),
              ReviewDisplayStatus.PUBLIC,
              "전체 흐름 테스트 리뷰",
              SizeType.GLASS,
              BigDecimal.valueOf(25000),
              createTestLocationInfo(),
              List.of(new ReviewImageInfoRequest(1L, viewUrl)),
              List.of(),
              4.0);

      MvcTestResult reviewResult =
          mockMvcTester
              .post()
              .uri("/api/v1/reviews")
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(reviewRequest))
              .with(csrf())
              .exchange();

      ReviewCreateResponse reviewResponse = extractData(reviewResult, ReviewCreateResponse.class);

      // ACTIVATED 상태로 변경 대기 (Single Record 방식: 레코드 수는 1개 유지)
      Awaitility.await()
          .atMost(5, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                assertEquals(1, logs.size());
                assertEquals(ResourceEventType.ACTIVATED, logs.get(0).getEventType());
              });

      // then - 단일 레코드 검증
      List<ResourceLog> allLogs = resourceLogRepository.findByUserId(userId);
      assertEquals(1, allLogs.size());

      ResourceLog resourceLog = allLogs.get(0);

      // ACTIVATED 상태 검증
      assertEquals(ResourceEventType.ACTIVATED, resourceLog.getEventType());
      assertEquals(reviewResponse.getId(), resourceLog.getReferenceId());
      assertEquals("REVIEW", resourceLog.getReferenceType());
      assertEquals(userId, resourceLog.getUserId());
      assertTrue(resourceLog.getResourceKey().startsWith("review/"));

      log.info(
          "전체 흐름 테스트 완료 - 레코드 ID: {}, 상태: {}", resourceLog.getId(), resourceLog.getEventType());
    }

    @Test
    @DisplayName("리뷰 수정 시 기존 이미지는 이미 ACTIVATED 상태이므로 상태가 유지되고, 새 이미지만 ACTIVATED로 변경된다")
    void test_modify_review_does_not_duplicate_activated_log() throws Exception {
      // given
      String token = getToken();
      Long userId = getTokenUserId();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      // 1. PreSigned URL 생성 (기존 이미지용)
      MvcTestResult presignResult =
          mockMvcTester
              .get()
              .uri("/api/v1/s3/presign-url")
              .param("rootPath", "review")
              .param("uploadSize", "1")
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      ImageUploadResponse uploadResponse = extractData(presignResult, ImageUploadResponse.class);
      String existingImageUrl = uploadResponse.imageUploadInfo().get(0).viewUrl();

      // CREATED 로그 저장 대기
      Awaitility.await()
          .atMost(3, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                assertEquals(1, logs.size());
              });

      // 2. 리뷰 생성 (이미지 1개 포함)
      ReviewCreateRequest createRequest =
          new ReviewCreateRequest(
              alcohol.getId(),
              ReviewDisplayStatus.PUBLIC,
              "최초 리뷰 내용",
              SizeType.GLASS,
              BigDecimal.valueOf(30000),
              createTestLocationInfo(),
              List.of(new ReviewImageInfoRequest(1L, existingImageUrl)),
              List.of(),
              4.0);

      MvcTestResult createResult =
          mockMvcTester
              .post()
              .uri("/api/v1/reviews")
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(createRequest))
              .with(csrf())
              .exchange();

      ReviewCreateResponse createResponse = extractData(createResult, ReviewCreateResponse.class);
      Long reviewId = createResponse.getId();

      // ACTIVATED 상태로 변경 대기 (Single Record 방식)
      Awaitility.await()
          .atMost(5, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                assertEquals(1, logs.size());
                assertEquals(ResourceEventType.ACTIVATED, logs.get(0).getEventType());
              });

      // 3. 새 이미지 PreSigned URL 생성
      MvcTestResult newPresignResult =
          mockMvcTester
              .get()
              .uri("/api/v1/s3/presign-url")
              .param("rootPath", "review")
              .param("uploadSize", "1")
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      ImageUploadResponse newUploadResponse =
          extractData(newPresignResult, ImageUploadResponse.class);
      String newImageUrl = newUploadResponse.imageUploadInfo().get(0).viewUrl();

      // 새 이미지 CREATED 로그 저장 대기 (총 2개: 기존 ACTIVATED 1개 + 새 CREATED 1개)
      Awaitility.await()
          .atMost(3, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                assertEquals(2, logs.size());
              });

      // 4. 리뷰 수정 (기존 이미지 + 새 이미지)
      ReviewModifyRequest modifyRequest =
          new ReviewModifyRequest(
              "수정된 리뷰 내용",
              ReviewDisplayStatus.PUBLIC,
              null,
              List.of(
                  new ReviewImageInfoRequest(1L, existingImageUrl),
                  new ReviewImageInfoRequest(2L, newImageUrl)),
              null,
              null,
              createTestLocationInfo());

      // when
      MvcTestResult modifyResult =
          mockMvcTester
              .patch()
              .uri("/api/v1/reviews/{reviewId}", reviewId)
              .header("Authorization", "Bearer " + token)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(modifyRequest))
              .with(csrf())
              .exchange();

      ReviewResultResponse modifyResponse = extractData(modifyResult, ReviewResultResponse.class);
      assertNotNull(modifyResponse);

      // then - 모든 로그가 ACTIVATED 상태로 변경됨 (총 2개 레코드 유지)
      Awaitility.await()
          .atMost(5, TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
                assertEquals(2, logs.size());
                long activatedCount =
                    logs.stream()
                        .filter(l -> l.getEventType() == ResourceEventType.ACTIVATED)
                        .count();
                assertEquals(2, activatedCount);
              });

      List<ResourceLog> allLogs = resourceLogRepository.findByUserId(userId);
      assertEquals(2, allLogs.size());
      assertTrue(allLogs.stream().allMatch(l -> l.getEventType() == ResourceEventType.ACTIVATED));
      assertTrue(allLogs.stream().allMatch(l -> reviewId.equals(l.getReferenceId())));

      log.info("리뷰 수정 후 총 로그 수: {}, 모두 ACTIVATED 상태", allLogs.size());
    }
  }
}
