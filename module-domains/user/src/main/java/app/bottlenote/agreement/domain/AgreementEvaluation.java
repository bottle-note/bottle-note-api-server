package app.bottlenote.agreement.domain;

import app.bottlenote.agreement.constant.AgreementType;
import java.time.LocalDateTime;
import java.util.List;

/** 사용자의 현재 약관 동의 판정 결과를 나타낸다. */
public record AgreementEvaluation(boolean eligible, List<Item> items) {

  public AgreementEvaluation {
    items = List.copyOf(items);
  }

  /** 약관 유형별 현재 동의 상태를 나타낸다. recordedAt은 이력 없으면 null. */
  public record Item(
      AgreementType type, boolean required, boolean agreed, LocalDateTime recordedAt) {}
}
