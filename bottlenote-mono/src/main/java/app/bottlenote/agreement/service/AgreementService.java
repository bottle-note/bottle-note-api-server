package app.bottlenote.agreement.service;

import app.bottlenote.agreement.domain.AgreementEvaluation;
import app.bottlenote.agreement.domain.AgreementSubmission;
import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.domain.UserAgreementRepository;
import app.bottlenote.agreement.exception.AgreementException;
import app.bottlenote.agreement.exception.AgreementExceptionCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자의 약관 동의 상태 조회와 의사표시 기록을 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgreementService {

  private final UserAgreementRepository userAgreementRepository;
  private final AgreementEvaluator agreementEvaluator;
  private final Clock clock;

  @Transactional(readOnly = true)
  public AgreementEvaluation getStatus(Long userId) {
    return agreementEvaluator.evaluate(userId);
  }

  @Transactional
  public AgreementEvaluation submit(
      Long userId, List<AgreementSubmission> submissions, String clientIp, String userAgent) {
    validate(submissions);
    // MySQL DATETIME(6)과 맞춰 마이크로초로 정규화한다. 나노 잔여 시 응답/DB 재조회 불일치가 난다.
    LocalDateTime recordedAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    submissions.forEach(
        submission ->
            userAgreementRepository.save(
                UserAgreement.create(
                    userId,
                    submission.type(),
                    submission.action(),
                    submission.content(),
                    recordedAt,
                    submission.inputContext(),
                    clientIp,
                    userAgent)));
    return agreementEvaluator.evaluate(userId);
  }

  private void validate(List<AgreementSubmission> submissions) {
    Objects.requireNonNull(submissions, "submissions는 null일 수 없습니다.");
    submissions.forEach(
        submission -> Objects.requireNonNull(submission, "submission은 null일 수 없습니다."));
    if (submissions.stream()
        .anyMatch(submission -> submission.content() == null || submission.content().isBlank())) {
      throw new AgreementException(AgreementExceptionCode.AGREEMENT_CONTENT_EMPTY);
    }
  }
}
