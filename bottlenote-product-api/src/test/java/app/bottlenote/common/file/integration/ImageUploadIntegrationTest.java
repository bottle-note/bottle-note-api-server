package app.bottlenote.common.file.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] ImageUpload")
class ImageUploadIntegrationTest extends IntegrationTestSupport {

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
      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/s3/presign-url")
                      .param("rootPath", rootPath)
                      .param("uploadSize", String.valueOf(uploadSize))
                      .header("Authorization", "Bearer " + getToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andExpect(jsonPath("$.data.uploadSize").value(uploadSize))
              .andExpect(jsonPath("$.data.imageUploadInfo").isArray())
              .andReturn();

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
      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/s3/presign-url")
                      .param("rootPath", rootPath)
                      .param("uploadSize", String.valueOf(uploadSize))
                      .header("Authorization", "Bearer " + getToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.uploadSize").value(uploadSize))
              .andExpect(jsonPath("$.data.imageUploadInfo").isArray())
              .andReturn();

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
      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/s3/presign-url")
                      .param("rootPath", rootPath)
                      .header("Authorization", "Bearer " + getToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.uploadSize").value(1))
              .andReturn();

      // then
      ImageUploadResponse response = extractData(result, ImageUploadResponse.class);
      assertEquals(1, response.uploadSize());
      assertEquals(1, response.imageUploadInfo().size());
    }
  }
}
