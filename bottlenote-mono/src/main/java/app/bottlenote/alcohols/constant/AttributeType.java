package app.bottlenote.alcohols.constant;

/** 위스키 속성의 분류. 화면 표기는 프론트가 붙이고 서버는 코드만 관리한다. */
public enum AttributeType {
  /** 추천 필터·키워드 그래프의 라벨 */
  KEYWORD,
  /** 대중성 점수 */
  POPULARITY,
  /** 병·잔 가격 */
  PRICE,
  /** 예/아니오 속성 */
  FLAG,
  /** 같은 브랜드 묶음 같은 식별 코드 */
  IDENTITY,
  /** 운영 메모 */
  NOTE
}
