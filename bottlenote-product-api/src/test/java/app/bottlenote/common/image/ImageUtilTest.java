package app.bottlenote.common.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [util] ImageUtil")
class ImageUtilTest {

  @Nested
  @DisplayName("extractResourceKey 메서드 테스트")
  class ExtractResourceKeyTest {

    @Test
    @DisplayName("CloudFront URL에서 리소스 키를 추출한다")
    void test_cloudfront_url() {
      // given
      String viewUrl = "https://cdn.bottlenote.com/review/20251231/1-uuid.jpg";

      // when
      String resourceKey = ImageUtil.extractResourceKey(viewUrl);

      // then
      assertEquals("review/20251231/1-uuid.jpg", resourceKey);
    }

    @Test
    @DisplayName("S3 URL에서 리소스 키를 추출한다")
    void test_s3_url() {
      // given
      String viewUrl =
          "https://bottlenote.s3.ap-northeast-2.amazonaws.com/profile/20251231/user-avatar.png";

      // when
      String resourceKey = ImageUtil.extractResourceKey(viewUrl);

      // then
      assertEquals("profile/20251231/user-avatar.png", resourceKey);
    }

    @Test
    @DisplayName("HTTP URL에서도 리소스 키를 추출한다")
    void test_http_url() {
      // given
      String viewUrl = "http://localhost:8080/help/20251231/image.jpg";

      // when
      String resourceKey = ImageUtil.extractResourceKey(viewUrl);

      // then
      assertEquals("help/20251231/image.jpg", resourceKey);
    }

    @Test
    @DisplayName("프로토콜이 없는 경로는 그대로 반환한다")
    void test_path_without_protocol() {
      // given
      String viewUrl = "review/20251231/1-uuid.jpg";

      // when
      String resourceKey = ImageUtil.extractResourceKey(viewUrl);

      // then
      assertEquals("review/20251231/1-uuid.jpg", resourceKey);
    }

    @Test
    @DisplayName("null 입력시 null을 반환한다")
    void test_null_input() {
      // when
      String resourceKey = ImageUtil.extractResourceKey(null);

      // then
      assertNull(resourceKey);
    }

    @Test
    @DisplayName("빈 문자열 입력시 null을 반환한다")
    void test_empty_string() {
      // when
      String resourceKey = ImageUtil.extractResourceKey("");

      // then
      assertNull(resourceKey);
    }

    @Test
    @DisplayName("공백 문자열 입력시 null을 반환한다")
    void test_blank_string() {
      // when
      String resourceKey = ImageUtil.extractResourceKey("   ");

      // then
      assertNull(resourceKey);
    }

    @Test
    @DisplayName("호스트만 있는 URL은 빈 문자열을 반환한다")
    void test_host_only_url() {
      // given
      String viewUrl = "https://cdn.bottlenote.com";

      // when
      String resourceKey = ImageUtil.extractResourceKey(viewUrl);

      // then
      assertEquals("", resourceKey);
    }

    @Test
    @DisplayName("깊은 경로의 URL에서 리소스 키를 추출한다")
    void test_deep_path_url() {
      // given
      String viewUrl = "https://cdn.bottlenote.com/a/b/c/d/e/image.jpg";

      // when
      String resourceKey = ImageUtil.extractResourceKey(viewUrl);

      // then
      assertEquals("a/b/c/d/e/image.jpg", resourceKey);
    }
  }
}
