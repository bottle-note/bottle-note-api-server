package app.bottlenote.agreement.constant;

import lombok.Getter;

/** 사용자가 동의할 수 있는 약관 유형을 나타낸다. */
@Getter
public enum AgreementType {
  TERMS_OF_SERVICE("서비스 이용약관", true),
  PRIVACY_COLLECTION_USE("개인정보 수집 및 이용 동의", true),
  MARKETING("마케팅 정보 수신 동의", false);

  private final String description;
  private final boolean required;

  AgreementType(String description, boolean required) {
    this.description = description;
    this.required = required;
  }
}
