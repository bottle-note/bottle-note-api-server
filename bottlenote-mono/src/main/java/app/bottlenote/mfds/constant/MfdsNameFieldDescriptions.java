package app.bottlenote.mfds.constant;

import java.util.Map;

/** 수입 신고 응답의 이름 필드 설명. 공유 DTO에는 문서 어노테이션을 두지 않으므로 각 API 모듈의 OpenAPI 설정이 이 설명을 스키마에 붙인다. */
public final class MfdsNameFieldDescriptions {

  private static final String SKU = " SKU는 용량·도수·숙성 연수가 다르면 구분되는 판매 단위다.";

  public static final Map<String, String> BY_PROPERTY =
      Map.of(
          "baseProductNameKo",
          "기본 제품명(한글). 식약처 원문 제품명에서 용량·도수·숙성 연수·LOT 같은 SKU 속성을 뺀 이름이다. 매칭과 관계없이 원문 기준으로 유지된다.",
          "baseProductNameEn",
          "기본 제품명(영문). 식약처 원문 제품명에서 용량·도수·숙성 연수·LOT 같은 SKU 속성을 뺀 이름이다. 매칭과 관계없이 원문 기준으로 유지된다.",
          "skuDisplayNameKo",
          "SKU 표시명(한글). 원문 제품명에서 포장 문구만 정리하고 이 신고의 용량·도수·숙성 연수는 남긴 이름이다. 신고마다 다르며 매칭으로 바뀌지 않는다."
              + SKU,
          "skuDisplayNameEn",
          "SKU 표시명(영문). 원문 제품명에서 포장 문구만 정리하고 이 신고의 용량·도수·숙성 연수는 남긴 이름이다. 신고마다 다르며 매칭으로 바뀌지 않는다."
              + SKU,
          "alcoholNameKo",
          "주류명(한글). 공개 수입 신고 화면이 표시하는 이름이다. 알코올이 매칭되면 매칭된 보틀노트 주류의 한글 이름으로 덮어쓴다. 관리자 확정은 확정 즉시, 자동 확정과 상속은 수집기 정제 실행 때 반영된다. 매칭 전에는 정제기가 만든 후보 이름이다.",
          "alcoholNameEn",
          "주류명(영문). 공개 수입 신고 화면이 표시하는 이름이다. 알코올이 매칭되면 매칭된 보틀노트 주류의 영문 이름으로 덮어쓴다. 관리자 확정은 확정 즉시, 자동 확정과 상속은 수집기 정제 실행 때 반영된다. 매칭 전에는 정제기가 만든 후보 이름이다.");

  private MfdsNameFieldDescriptions() {}
}
