package app.bottlenote.alcohols.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class AlcoholAttributeConstraintViolationTest {
  @ParameterizedTest
  @CsvSource({
    "1062,uk_alcohol_attributes_key,true",
    "1062,alcohol_attributes.uk_alcohol_attributes_display_name,true",
    "1062,another_unique_key,false",
    "1452,uk_alcohol_attributes_key,false",
    "1062,,false"
  })
  @DisplayName("DB 예외를 변환할 때 속성 사전의 고유 제약 위반만 중복으로 판단한다")
  void 속성_고유_제약만_판별한다(int errorCode, String constraint, boolean expected) {
    var sql = new SQLException("uk_alcohol_attributes_key", "23000", errorCode);
    var cause = new ConstraintViolationException("duplicate", sql, constraint);
    assertThat(AlcoholAttributeConstraintViolation.isDuplicate(new RuntimeException(cause)))
        .isEqualTo(expected);
  }
}
