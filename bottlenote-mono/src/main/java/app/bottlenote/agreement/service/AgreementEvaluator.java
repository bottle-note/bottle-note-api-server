package app.bottlenote.agreement.service;

import app.bottlenote.agreement.config.AgreementPolicyProperties;
import app.bottlenote.agreement.config.AgreementPolicyProperties.Policy;
import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.AgreementEvaluation;
import app.bottlenote.agreement.domain.AgreementEvaluation.Item;
import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.domain.UserAgreementRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgreementEvaluator {

  private final UserAgreementRepository userAgreementRepository;
  private final AgreementPolicyProperties policyProperties;

  public AgreementEvaluation evaluate(Long userId) {
    List<Item> items =
        Arrays.stream(AgreementType.values()).map(type -> evaluate(userId, type)).toList();
    boolean eligible = items.stream().filter(Item::required).allMatch(Item::agreed);
    return new AgreementEvaluation(eligible, items);
  }

  private Item evaluate(Long userId, AgreementType type) {
    Policy policy = policyProperties.getPolicy(type);
    boolean agreed =
        userAgreementRepository
            .findFirstByUserIdAndAgreementTypeOrderByRecordedAtDescIdDesc(userId, type)
            .filter(agreement -> isEffectiveAgreement(agreement, policy))
            .isPresent();
    return new Item(type, policy.isRequired(), agreed);
  }

  private boolean isEffectiveAgreement(UserAgreement agreement, Policy policy) {
    return agreement.getAction() == AgreementAction.AGREE
        && !agreement.getRecordedAt().isBefore(policy.getEffectiveFrom());
  }
}
