package app.bottlenote.agreement.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.agreement.config.AgreementPolicyProperties;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AgreementEvaluator 단위 테스트")
class AgreementEvaluatorTest {

  private static final Long USER_ID = 1L;
  private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.of(2026, 8, 1, 0, 0);

  private InMemoryUserAgreementRepository repository;
  private AgreementPolicyProperties policyProperties;
  private AgreementEvaluator evaluator;

  @BeforeEach
  void setUp() {
    repository = new InMemoryUserAgreementRepository();
    policyProperties = new AgreementPolicyProperties();
    evaluator = new AgreementEvaluator(repository, policyProperties);
  }

  @Nested
  @DisplayName("필수 동의 상태를 평가할 때")
  class EvaluateRequiredAgreements {

    @Test
    @DisplayName("이력이 없으면 모든 항목을 미동의로 판정한다")
    void evaluate_whenHistoryDoesNotExist_returnsNotEligible() {
      AgreementEvaluation result = evaluator.evaluate(USER_ID);

      assertThat(result.eligible()).isFalse();
      assertThat(result.items())
          .containsExactly(
              new Item(AgreementType.TERMS_OF_SERVICE, true, false),
              new Item(AgreementType.PRIVACY_COLLECTION_USE, true, false));
    }

    @Test
    @DisplayName("두 필수 유형의 최신 동의가 기준 시각 이상이면 충족으로 판정한다")
    void evaluate_whenAllRequiredAgreementsAreEffective_returnsEligible() {
      save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, EFFECTIVE_FROM);
      save(
          AgreementType.PRIVACY_COLLECTION_USE,
          AgreementAction.AGREE,
          EFFECTIVE_FROM.plusSeconds(1));

      AgreementEvaluation result = evaluator.evaluate(USER_ID);

      assertThat(result.eligible()).isTrue();
      assertThat(result.items()).allMatch(Item::agreed);
    }

    @Test
    @DisplayName("최신 이력이 철회면 기준 시각 이후 동의 이력이 있어도 미충족으로 판정한다")
    void evaluate_whenLatestActionIsRevoke_returnsNotEligible() {
      save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, EFFECTIVE_FROM.plusMinutes(1));
      save(AgreementType.TERMS_OF_SERVICE, AgreementAction.REVOKE, EFFECTIVE_FROM.plusMinutes(2));
      save(
          AgreementType.PRIVACY_COLLECTION_USE,
          AgreementAction.AGREE,
          EFFECTIVE_FROM.plusMinutes(1));

      AgreementEvaluation result = evaluator.evaluate(USER_ID);

      assertThat(result.eligible()).isFalse();
      assertThat(item(result, AgreementType.TERMS_OF_SERVICE).agreed()).isFalse();
    }

    @Test
    @DisplayName("최신 동의가 기준 시각 이전이면 미충족으로 판정한다")
    void evaluate_whenLatestAgreementIsBeforeEffectiveFrom_returnsNotEligible() {
      save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, EFFECTIVE_FROM.minusNanos(1));
      save(
          AgreementType.PRIVACY_COLLECTION_USE,
          AgreementAction.AGREE,
          EFFECTIVE_FROM.plusMinutes(1));

      AgreementEvaluation result = evaluator.evaluate(USER_ID);

      assertThat(result.eligible()).isFalse();
      assertThat(item(result, AgreementType.TERMS_OF_SERVICE).agreed()).isFalse();
    }

    @Test
    @DisplayName("철회 이후 다시 유효하게 동의하면 충족으로 판정한다")
    void evaluate_whenLatestActionReturnsToAgree_returnsEligible() {
      save(AgreementType.TERMS_OF_SERVICE, AgreementAction.REVOKE, EFFECTIVE_FROM.plusMinutes(1));
      save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, EFFECTIVE_FROM.plusMinutes(2));
      save(
          AgreementType.PRIVACY_COLLECTION_USE,
          AgreementAction.AGREE,
          EFFECTIVE_FROM.plusMinutes(1));

      AgreementEvaluation result = evaluator.evaluate(USER_ID);

      assertThat(result.eligible()).isTrue();
    }

    @Test
    @DisplayName("기록 시각이 같으면 더 큰 ID의 이력을 최신으로 판정한다")
    void evaluate_whenRecordedAtIsSame_usesGreaterIdAsLatest() {
      save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, EFFECTIVE_FROM);
      save(AgreementType.TERMS_OF_SERVICE, AgreementAction.REVOKE, EFFECTIVE_FROM);
      save(AgreementType.PRIVACY_COLLECTION_USE, AgreementAction.AGREE, EFFECTIVE_FROM);

      AgreementEvaluation result = evaluator.evaluate(USER_ID);

      assertThat(result.eligible()).isFalse();
      assertThat(item(result, AgreementType.TERMS_OF_SERVICE).agreed()).isFalse();
    }
  }

  @Test
  @DisplayName("선택 유형의 미동의 상태는 전체 충족 여부에 영향을 주지 않는다")
  void evaluate_whenOptionalAgreementIsMissing_returnsEligible() {
    policyProperties.getPrivacyCollectionUse().setRequired(false);
    save(AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, EFFECTIVE_FROM);

    AgreementEvaluation result = evaluator.evaluate(USER_ID);

    assertThat(result.eligible()).isTrue();
    assertThat(item(result, AgreementType.PRIVACY_COLLECTION_USE))
        .isEqualTo(new Item(AgreementType.PRIVACY_COLLECTION_USE, false, false));
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
