package app.bottlenote.campaigncontent.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

@Tag("unit")
@DisplayName("캠페인 콘텐츠 DB 제약 분류기")
class CampaignContentConstraintViolationClassifierTest {

  private static final String FOREIGN_KEY = "fk_campaign_content_events_content";

  @Test
  @DisplayName("Hibernate가 알려진 제약 이름을 주면 정확히 식별한다")
  void 알려진_제약_이름을_식별한다() {
    assertThat(
            CampaignContentConstraintViolationClassifier.matches(
                violation(FOREIGN_KEY, "message"), FOREIGN_KEY))
        .isTrue();
  }

  @Test
  @DisplayName("Hibernate가 테이블 한정 제약 이름을 주면 정규화해 식별한다")
  void 테이블_한정_제약_이름을_식별한다() {
    assertThat(
            CampaignContentConstraintViolationClassifier.matches(
                violation("`campaign_content_events`.`" + FOREIGN_KEY + "`", "message"),
                FOREIGN_KEY))
        .isTrue();
  }

  @Test
  @DisplayName("Hibernate 제약 이름이 null이면 MySQL CONSTRAINT 식별자를 파싱한다")
  void 이름이_null이면_MySQL_제약_식별자를_파싱한다() {
    String message =
        "Cannot add or update a child row: a foreign key constraint fails "
            + "(`bottlenote`.`campaign_content_events`, CONSTRAINT `"
            + FOREIGN_KEY
            + "` FOREIGN KEY (`campaign_content_id`) REFERENCES `campaign_contents` (`id`))";

    assertThat(
            CampaignContentConstraintViolationClassifier.matches(
                violation(null, message), FOREIGN_KEY))
        .isTrue();
  }

  @Test
  @DisplayName("비슷한 접두사의 다른 제약 이름은 식별하지 않는다")
  void 비슷한_접두사_제약은_식별하지_않는다() {
    assertThat(
            CampaignContentConstraintViolationClassifier.matches(
                violation(FOREIGN_KEY + "_archive", "message"), FOREIGN_KEY))
        .isFalse();
  }

  @Test
  @DisplayName("메시지 값에 알려진 문자열만 있으면 제약으로 식별하지 않는다")
  void 메시지_값의_문자열은_제약으로_식별하지_않는다() {
    String message = "Duplicate entry '" + FOREIGN_KEY + "' for key 'another_constraint'";

    assertThat(
            CampaignContentConstraintViolationClassifier.matches(
                violation(null, message), FOREIGN_KEY))
        .isFalse();
  }

  @Test
  @DisplayName("Hibernate 제약 예외가 원인 체인에 없으면 식별하지 않는다")
  void Hibernate_제약_원인이_없으면_식별하지_않는다() {
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException("CONSTRAINT `" + FOREIGN_KEY + "`");

    assertThat(CampaignContentConstraintViolationClassifier.matches(exception, FOREIGN_KEY))
        .isFalse();
  }

  private static DataIntegrityViolationException violation(
      String constraintName, String sqlMessage) {
    ConstraintViolationException hibernateException =
        new ConstraintViolationException(
            "could not execute statement", new SQLException(sqlMessage), constraintName);
    return new DataIntegrityViolationException("data integrity violation", hibernateException);
  }
}
