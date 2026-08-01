package app.bottlenote.agreement.constant;

import lombok.Getter;

/** 사용자가 동의할 수 있는 약관 유형을 나타낸다. */
@Getter
public enum AgreementType {
  TERMS_OF_SERVICE("서비스 이용약관"),
  PRIVACY_COLLECTION_USE("개인정보 수집 및 이용 동의");

  private final String description;

  AgreementType(String description) {
    this.description = description;
  }
}
