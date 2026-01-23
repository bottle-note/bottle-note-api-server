package app.bottlenote.common.file.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [event] ImageResourceActivatedEvent")
class ImageResourceActivatedEventTest {

  @Nested
  @DisplayName("이벤트 생성 테스트")
  class CreateEventTest {

    @Test
    @DisplayName("단일 리소스 키로 이벤트를 생성할 수 있다")
    void test_create_with_single_key() {
      // given
      String resourceKey = "review/20251231/1-uuid.jpg";
      Long referenceId = 100L;
      String referenceType = "REVIEW";

      // when
      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(resourceKey, referenceId, referenceType);

      // then
      assertEquals(1, event.resourceKeys().size());
      assertEquals(resourceKey, event.resourceKeys().get(0));
      assertEquals(referenceId, event.referenceId());
      assertEquals(referenceType, event.referenceType());
    }

    @Test
    @DisplayName("여러 리소스 키로 이벤트를 생성할 수 있다")
    void test_create_with_multiple_keys() {
      // given
      List<String> resourceKeys =
          List.of(
              "review/20251231/1-uuid1.jpg",
              "review/20251231/2-uuid2.jpg",
              "review/20251231/3-uuid3.jpg");
      Long referenceId = 200L;
      String referenceType = "REVIEW";

      // when
      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(resourceKeys, referenceId, referenceType);

      // then
      assertEquals(3, event.resourceKeys().size());
      assertEquals(resourceKeys, event.resourceKeys());
      assertEquals(referenceId, event.referenceId());
      assertEquals(referenceType, event.referenceType());
    }

    @Test
    @DisplayName("resourceKeys가 null이면 NullPointerException이 발생한다")
    void test_null_resource_keys() {
      // when & then
      assertThrows(
          NullPointerException.class,
          () -> ImageResourceActivatedEvent.of((List<String>) null, 100L, "REVIEW"));
    }

    @Test
    @DisplayName("referenceId가 null이면 NullPointerException이 발생한다")
    void test_null_reference_id() {
      // when & then
      assertThrows(
          NullPointerException.class, () -> ImageResourceActivatedEvent.of("key", null, "REVIEW"));
    }

    @Test
    @DisplayName("referenceType이 null이면 NullPointerException이 발생한다")
    void test_null_reference_type() {
      // when & then
      assertThrows(
          NullPointerException.class, () -> ImageResourceActivatedEvent.of("key", 100L, null));
    }
  }

  @Nested
  @DisplayName("참조 타입별 이벤트 생성 테스트")
  class ReferenceTypeTest {

    @Test
    @DisplayName("PROFILE 타입의 이벤트를 생성할 수 있다")
    void test_profile_type() {
      // when
      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of("profile/user/avatar.jpg", 1L, "PROFILE");

      // then
      assertEquals("PROFILE", event.referenceType());
    }

    @Test
    @DisplayName("HELP 타입의 이벤트를 생성할 수 있다")
    void test_help_type() {
      // when
      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of("help/20251231/image.jpg", 50L, "HELP");

      // then
      assertEquals("HELP", event.referenceType());
    }

    @Test
    @DisplayName("BUSINESS 타입의 이벤트를 생성할 수 있다")
    void test_business_type() {
      // when
      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of("business/20251231/doc.jpg", 30L, "BUSINESS");

      // then
      assertEquals("BUSINESS", event.referenceType());
    }
  }
}
