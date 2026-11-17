package app.bottlenote.alcohols.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttributeOperator implements CodeLabel {
  EQ("같음", "equal", "술별 속성 값이 검색 값과 같은 항목을 조회한다"),
  GTE("이상", "greater_or_equal", "술별 숫자 속성 값이 검색 값 이상인 항목을 조회한다");

  private final String korName;
  private final String engName;
  private final String description;
}
