package app.bottlenote.alcohols.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttributeType {
  KEYWORD("키워드", "keyword", "추천 필터와 키워드 그래프에 쓰는 라벨", ValueProvider.ANY),
  POPULARITY("대중성 점수", "popularity", "관리자가 시즌에 맞게 조절해서 넣는 대중성 점수", ValueProvider.ANY),
  PRICE("가격", "price", "병·잔 가격. 출처와 기준 시점을 함께 남긴다", ValueProvider.ANY),
  FLAG("예/아니오 속성", "flag", "향 첨가 여부처럼 참·거짓으로 나뉘는 속성", ValueProvider.ADMIN),
  IDENTITY("묶음 코드", "identity", "같은 브랜드처럼 여러 병을 하나로 묶는 식별 코드", ValueProvider.ANY),
  NOTE("메모", "note", "운영자가 남기는 자유 메모", ValueProvider.ADMIN);

  private final String korName;
  private final String engName;
  private final String description;
  private final ValueProvider provider;
}
