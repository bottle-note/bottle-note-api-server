package app.bottlenote.agreement.constant;

import lombok.Getter;

/** 사용자의 약관 동의 의사표시를 나타낸다. */
@Getter
public enum AgreementAction {
  AGREE("약관에 동의함"),
  REVOKE("약관 동의를 철회함");

  private final String description;

  AgreementAction(String description) {
    this.description = description;
  }
}
