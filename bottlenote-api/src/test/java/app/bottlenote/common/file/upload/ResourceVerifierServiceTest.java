package app.bottlenote.common.file.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.common.file.dto.request.ResourceLogRequest;
import app.bottlenote.common.file.exception.FileException;
import app.bottlenote.common.file.service.ResourceCommandService;
import app.bottlenote.common.file.service.ResourceVerifierService;
import app.bottlenote.common.file.upload.fixture.InMemoryResourceLogRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] ResourceVerifierService")
class ResourceVerifierServiceTest {

  private ResourceCommandService resourceCommandService;
  private ResourceVerifierService resourceVerifierService;

  @BeforeEach
  void setUp() {
    InMemoryResourceLogRepository resourceLogRepository = new InMemoryResourceLogRepository();
    resourceCommandService = new ResourceCommandService(resourceLogRepository);
    resourceVerifierService = new ResourceVerifierService(resourceLogRepository);
  }

  @Test
  @DisplayName("현재 사용자가 발급받은 CREATED 리소스면 검증에 성공한다")
  void verifyOwnedImageResources_whenCreatedByUser_returnsResourceKeys() {
    // given
    String viewUrl = "https://cdn.bottlenote.com/review/20260522/1-image.jpg";
    saveCreated(1L, "review/20260522/1-image.jpg", viewUrl);

    // when
    List<String> result =
        resourceVerifierService.verifyOwnedImageResources(List.of(viewUrl), 1L, null, "REVIEW");

    // then
    assertThat(result).containsExactly("review/20260522/1-image.jpg");
  }

  @Test
  @DisplayName("이미 같은 참조에 연결된 ACTIVATED 리소스면 검증에 성공한다")
  void verifyOwnedImageResources_whenActivatedForSameReference_returnsResourceKeys() {
    // given
    String resourceKey = "review/20260522/1-image.jpg";
    String viewUrl = "https://cdn.bottlenote.com/" + resourceKey;
    saveCreated(1L, resourceKey, viewUrl);
    resourceCommandService.activateImageResource(resourceKey, 10L, "REVIEW", 1L).join();

    // when
    List<String> result =
        resourceVerifierService.verifyOwnedImageResources(List.of(viewUrl), 1L, 10L, "REVIEW");

    // then
    assertThat(result).containsExactly(resourceKey);
  }

  @Test
  @DisplayName("등록되지 않은 viewUrl이면 검증에 실패한다")
  void verifyOwnedImageResources_whenResourceLogMissing_throwsException() {
    assertThatThrownBy(
            () ->
                resourceVerifierService.verifyOwnedImageResources(
                    List.of("https://cdn.bottlenote.com/review/missing.jpg"), 1L, null, "REVIEW"))
        .isInstanceOf(FileException.class);
  }

  @Test
  @DisplayName("다른 사용자가 발급받은 viewUrl이면 검증에 실패한다")
  void verifyOwnedImageResources_whenOwnerMismatch_throwsException() {
    // given
    String viewUrl = "https://cdn.bottlenote.com/review/20260522/1-image.jpg";
    saveCreated(1L, "review/20260522/1-image.jpg", viewUrl);

    // when & then
    assertThatThrownBy(
            () ->
                resourceVerifierService.verifyOwnedImageResources(
                    List.of(viewUrl), 2L, null, "REVIEW"))
        .isInstanceOf(FileException.class);
  }

  @Test
  @DisplayName("저장된 viewUrl과 호스트가 다르면 검증에 실패한다")
  void verifyOwnedImageResources_whenViewUrlMismatch_throwsException() {
    // given
    String resourceKey = "review/20260522/1-image.jpg";
    saveCreated(1L, resourceKey, "https://cdn.bottlenote.com/" + resourceKey);

    // when & then
    assertThatThrownBy(
            () ->
                resourceVerifierService.verifyOwnedImageResources(
                    List.of("https://evil.example.com/" + resourceKey), 1L, null, "REVIEW"))
        .isInstanceOf(FileException.class);
  }

  @Test
  @DisplayName("이미 다른 참조에 연결된 리소스면 검증에 실패한다")
  void verifyOwnedImageResources_whenAlreadyUsedByOtherReference_throwsException() {
    // given
    String resourceKey = "review/20260522/1-image.jpg";
    String viewUrl = "https://cdn.bottlenote.com/" + resourceKey;
    saveCreated(1L, resourceKey, viewUrl);
    resourceCommandService.activateImageResource(resourceKey, 10L, "REVIEW", 1L).join();

    // when & then
    assertThatThrownBy(
            () ->
                resourceVerifierService.verifyOwnedImageResources(
                    List.of(viewUrl), 1L, 11L, "REVIEW"))
        .isInstanceOf(FileException.class);
  }

  private void saveCreated(Long userId, String resourceKey, String viewUrl) {
    resourceCommandService.saveImageResourceCreated(
        new ResourceLogRequest(userId, resourceKey, viewUrl, "review", "test-bucket"));
    assertThat(resourceCommandService.findByResourceKey(resourceKey))
        .isPresent()
        .get()
        .satisfies(response -> assertThat(response.eventType().name()).isEqualTo("CREATED"));
  }
}
