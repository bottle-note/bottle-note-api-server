package app.bottlenote.global.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.dto.request.AdminBulkReorderRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AdminBulkReorderRequest 단위 테스트")
class AdminBulkReorderRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  @DisplayName("ids가 100개일 때 검증에 성공한다")
  void validate_whenIdsSizeIsOneHundred_succeeds() {
    AdminBulkReorderRequest request = new AdminBulkReorderRequest(ids(100));

    Set<ConstraintViolation<AdminBulkReorderRequest>> violations = validator.validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("ids가 비어 있을 때 검증에 실패한다")
  void validate_whenIdsIsEmpty_fails() {
    AdminBulkReorderRequest request = new AdminBulkReorderRequest(List.of());

    Set<ConstraintViolation<AdminBulkReorderRequest>> violations = validator.validate(request);

    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .contains("BULK_REORDER_IDS_REQUIRED");
  }

  @Test
  @DisplayName("ids가 100개를 초과할 때 검증에 실패한다")
  void validate_whenIdsSizeExceedsOneHundred_fails() {
    AdminBulkReorderRequest request = new AdminBulkReorderRequest(ids(101));

    Set<ConstraintViolation<AdminBulkReorderRequest>> violations = validator.validate(request);

    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .contains("BULK_REORDER_IDS_MAX_SIZE");
  }

  @Test
  @DisplayName("ids에 null이 포함될 때 검증에 실패한다")
  void validate_whenIdIsNull_fails() {
    List<Long> ids = new ArrayList<>();
    ids.add(1L);
    ids.add(null);

    AdminBulkReorderRequest request = new AdminBulkReorderRequest(ids);

    Set<ConstraintViolation<AdminBulkReorderRequest>> violations = validator.validate(request);

    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .contains("BULK_REORDER_ID_REQUIRED");
  }

  @Test
  @DisplayName("ids에 0 이하 값이 포함될 때 검증에 실패한다")
  void validate_whenIdIsLessThanOne_fails() {
    AdminBulkReorderRequest request = new AdminBulkReorderRequest(List.of(1L, 0L));

    Set<ConstraintViolation<AdminBulkReorderRequest>> violations = validator.validate(request);

    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .contains("BULK_REORDER_ID_MINIMUM");
  }

  private List<Long> ids(int size) {
    List<Long> ids = new ArrayList<>();
    for (long id = 1; id <= size; id++) {
      ids.add(id);
    }
    return ids;
  }
}
