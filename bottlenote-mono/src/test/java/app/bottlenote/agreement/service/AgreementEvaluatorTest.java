package app.bottlenote.agreement.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.AgreementEvaluation;
import app.bottlenote.agreement.domain.AgreementEvaluation.Item;
import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.fixture.InMemoryUserAgreementRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AgreementEvaluator 단위 테스트")
class AgreementEvaluatorTest {

  private static final Long USER_ID = 1L;
  private static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 8, 1, 0, 0);

  private InMemoryUserAgreementRepository repository;
  private AgreementEvaluator evaluator;

  @BeforeEach
  void setUp() {
    repository = new InMemoryUserAgreementRepository();
    evaluator = new AgreementEvaluator(repository);
  }

  @Test
  @DisplayName("이력이 없으면 모든 항목을 미동의로 판정한다")
  void evaluate_whenHistoryDoesNotExist_returnsNotEligible() {
    AgreementEvaluation result = evaluator.evaluate(USER_ID);

    assertThat(result.eligible()).isFalse();
    assertThat(result.items())
        .containsExactly(
            new Item(AgreementType.TERMS_OF_SERVICE, true, false),
            new Item(AgreementType.PRIVACY_COLLECTION_USE, true, false),
            new Item(AgreementType.MARKETING, false, false));
  }

  @Test
  @DisplayName("두 유형의 최신 이력이 동의면 충족으로 판정한다")
  void evaluate_whenLatestActionsAreAgree_returnsEligible() {
    save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, RECORDED_AT);
    save(AgreementType.PRIVACY_COLLECTION_USE, AgreementAction.AGREE, RECORDED_AT.minusYears(1));

    AgreementEvaluation result = evaluator.evaluate(USER_ID);

    assertThat(result.eligible()).isTrue();
    assertThat(result.items()).filteredOn(Item::required).allMatch(Item::agreed);
    assertThat(item(result, AgreementType.MARKETING).agreed()).isFalse();
  }

  @Test
  @DisplayName("동의 후 철회하면 기록 시각과 관계없이 철회로 판정한다")
  void evaluate_whenLatestActionIsRevoke_returnsNotEligible() {
    save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, RECORDED_AT.plusSeconds(10));
    save(AgreementType.TERMS_OF_SERVICE, AgreementAction.REVOKE, RECORDED_AT);
    save(AgreementType.PRIVACY_COLLECTION_USE, AgreementAction.AGREE, RECORDED_AT);

    AgreementEvaluation result = evaluator.evaluate(USER_ID);

    assertThat(result.eligible()).isFalse();
    assertThat(item(result, AgreementType.TERMS_OF_SERVICE).agreed()).isFalse();
  }

  @Test
  @DisplayName("철회 후 다시 동의하면 최신 동의 상태로 판정한다")
  void evaluate_whenLatestActionReturnsToAgree_returnsEligible() {
    save(AgreementType.TERMS_OF_SERVICE, AgreementAction.REVOKE, RECORDED_AT.plusSeconds(10));
    save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, RECORDED_AT);
    save(AgreementType.PRIVACY_COLLECTION_USE, AgreementAction.AGREE, RECORDED_AT);

    AgreementEvaluation result = evaluator.evaluate(USER_ID);

    assertThat(result.eligible()).isTrue();
  }

  private void save(AgreementType type, AgreementAction action, LocalDateTime recordedAt) {
    repository.save(
        UserAgreement.create(
            USER_ID,
            type,
            action,
            "document",
            recordedAt,
            AgreementInputContext.INDIVIDUAL,
            "127.0.0.1",
            "test-agent"));
  }

  private Item item(AgreementEvaluation evaluation, AgreementType type) {
    return evaluation.items().stream()
        .filter(item -> item.type() == type)
        .findFirst()
        .orElseThrow();
  }
}
