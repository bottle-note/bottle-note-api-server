package app.bottlenote.agreement.domain;

import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import java.util.Objects;

/** 사용자가 제출한 약관 동의 또는 철회 정보를 나타낸다. */
public record AgreementSubmission(
    AgreementType type,
    AgreementAction action,
    String content,
    AgreementInputContext inputContext) {

  public AgreementSubmission {
    Objects.requireNonNull(type, "type은 null일 수 없습니다.");
    Objects.requireNonNull(action, "action은 null일 수 없습니다.");
    Objects.requireNonNull(inputContext, "inputContext는 null일 수 없습니다.");
  }
}
