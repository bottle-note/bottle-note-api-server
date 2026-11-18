package app.bottlenote.alcohols.constant;

public enum AttributeSearchOperator {
  EQ,
  NE,
  GT,
  GTE,
  LT,
  LTE,
  CONTAINS,
  EXISTS,
  NOT_EXISTS;

  public boolean requiresValue() {
    return this != EXISTS && this != NOT_EXISTS;
  }

  public boolean isNumericComparison() {
    return this == GT || this == GTE || this == LT || this == LTE;
  }
}
