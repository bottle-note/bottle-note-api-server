package app.bottlenote.alcohols.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttributeOperator implements CodeLabel {
  EQ("같음", "equal", "속성 값과 같음을 나타내며 연결은 관리자가 등록한다"),
  GTE("이상", "greater_or_equal", "숫자 속성 값 이상을 나타내며 자동 연결하지 않는다");

  private final String korName;
  private final String engName;
  private final String description;
}
