package app.bottlenote.agreement.service;

import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.AgreementEvaluation;
import app.bottlenote.agreement.domain.AgreementEvaluation.Item;
import app.bottlenote.agreement.domain.UserAgreementRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 사용자의 최신 약관 이력을 기준으로 현재 동의 상태를 판정한다. */
@Component
@RequiredArgsConstructor
public class AgreementEvaluator {

  private final UserAgreementRepository userAgreementRepository;

  public AgreementEvaluation evaluate(Long userId) {
    List<Item> items =
        Arrays.stream(AgreementType.values()).map(type -> evaluate(userId, type)).toList();
    boolean eligible = items.stream().filter(Item::required).allMatch(Item::agreed);
    return new AgreementEvaluation(eligible, items);
  }

  private Item evaluate(Long userId, AgreementType type) {
    boolean agreed =
        userAgreementRepository
            .findFirstByUserIdAndAgreementTypeOrderByIdDesc(userId, type)
            .filter(agreement -> agreement.getAction() == AgreementAction.AGREE)
            .isPresent();
    return new Item(type, type.isRequired(), agreed);
  }
}
