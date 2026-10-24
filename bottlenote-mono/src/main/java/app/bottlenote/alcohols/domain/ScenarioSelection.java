package app.bottlenote.alcohols.domain;

/** 서명한 선택 조건은 후보 분포가 달라져도 같은 의미로 평가한다. */
public record ScenarioSelection(ScenarioAnswer answer, boolean minExclusive, String label) {}
