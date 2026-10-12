package app.bottlenote.alcohols.constant;

/** 화면 표기(korName·engName·description)를 가진 enum의 공통 계약. 응답으로 평탄화할 때 쓴다. */
public interface CodeLabel {
  String name();

  String getKorName();

  String getEngName();

  String getDescription();
}
