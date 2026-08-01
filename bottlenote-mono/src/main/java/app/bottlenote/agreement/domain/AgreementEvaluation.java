package app.bottlenote.agreement.domain;

import app.bottlenote.agreement.constant.AgreementType;
import java.util.List;

public record AgreementEvaluation(boolean eligible, List<Item> items) {

  public AgreementEvaluation {
    items = List.copyOf(items);
  }

  public record Item(AgreementType type, boolean required, boolean agreed) {}
}
