package app.bottlenote.statistics.fixture;

import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 회원 통계 통합 테스트용 원천 데이터. 가입일과 롤업 행은 JPA 감사 필드를 우회해 직접 쓴다. */
@Component
@RequiredArgsConstructor
public class MemberStatisticsTestFactory {

  private final JdbcTemplate jdbcTemplate;

  /** users.create_at은 감사 리스너가 채우므로 코호트 테스트는 저장 후 가입일을 덮어쓴다. */
  public void updateSignedUpAt(Long userId, LocalDateTime signedUpAt) {
    jdbcTemplate.update("UPDATE users SET create_at = ? WHERE id = ?", signedUpAt, userId);
  }

  public void persistDailyActivity(Long userId, LocalDate activityDate) {
    LocalDateTime seenAt = activityDate.atTime(12, 0);
    jdbcTemplate.update(
        """
        INSERT INTO user_daily_activities
            (user_id, activity_date, request_count, first_seen_at, last_seen_at, recorded_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        userId,
        activityDate,
        1,
        seenAt,
        seenAt,
        seenAt);
  }

  public void persistAgreement(
      Long userId, AgreementType type, AgreementAction action, LocalDateTime recordedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO user_agreements
            (user_id, agreement_type, action, document_content, recorded_at, input_context)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        userId,
        type.name(),
        action.name(),
        "test-document",
        recordedAt,
        AgreementInputContext.BULK.name());
  }

  /** 필수 약관 전체를 동의 상태로 만든다. */
  public void agreeRequired(Long userId, LocalDateTime recordedAt) {
    for (AgreementType type : AgreementType.values()) {
      if (type.isRequired()) {
        persistAgreement(userId, type, AgreementAction.AGREE, recordedAt);
      }
    }
  }
}
