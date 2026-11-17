package app.bottlenote.agreement.constant;

import lombok.Getter;

/** 사용자가 동의 의사를 입력한 방식을 나타낸다. */
@Getter
public enum AgreementInputContext {
  INDIVIDUAL("개별 동의"),
  BULK("일괄 동의");

  private final String description;

  AgreementInputContext(String description) {
    this.description = description;
  }
}
