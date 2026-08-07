package app.bottlenote.agreement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.AgreementEvaluation;
import app.bottlenote.agreement.domain.AgreementEvaluation.Item;
import app.bottlenote.agreement.domain.AgreementSubmission;
import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.exception.AgreementException;
import app.bottlenote.agreement.exception.AgreementExceptionCode;
import app.bottlenote.agreement.fixture.InMemoryUserAgreementRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AgreementService 단위 테스트")
class AgreementServiceTest {

  private static final Long USER_ID = 1L;
  private static final String CLIENT_IP = "203.0.113.10";
  private static final String USER_AGENT = "BottleNote/1.0";
  private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final Instant RECORDED_INSTANT = Instant.parse("2026-08-01T01:23:45Z");
  private static final LocalDateTime RECORDED_AT =
      LocalDateTime.ofInstant(RECORDED_INSTANT, ZONE_ID);

  private InMemoryUserAgreementRepository repository;
  private AgreementService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryUserAgreementRepository();
    AgreementEvaluator evaluator = new AgreementEvaluator(repository);
    Clock clock = Clock.fixed(RECORDED_INSTANT, ZONE_ID);
    service = new AgreementService(repository, evaluator, clock);
  }

  @Test
  @DisplayName("동의 상태를 조회할 때 Evaluator의 공통 결과를 반환한다")
  void getStatus_whenHistoryDoesNotExist_returnsEvaluation() {
    AgreementEvaluation result = service.getStatus(USER_ID);

    assertThat(result.eligible()).isFalse();
    assertThat(result.items())
        .containsExactly(
            new Item(AgreementType.TERMS_OF_SERVICE, true, false, null),
            new Item(AgreementType.PRIVACY_COLLECTION_USE, true, false, null),
            new Item(AgreementType.MARKETING, false, false, null));
  }

  @Nested
  @DisplayName("동의를 제출할 때")
  class SubmitAgreements {

    @Test
    @DisplayName("항목별 이벤트와 서버 메타데이터를 append-only로 기록한다")
    void submit_whenIndividualItemsAreSubmitted_recordsEveryEvent() {
      List<AgreementSubmission> submissions = requiredSubmissions(AgreementInputContext.INDIVIDUAL);

      service.submit(USER_ID, submissions, CLIENT_IP, USER_AGENT);

      assertThat(repository.findAll())
          .hasSize(2)
          .allSatisfy(
              agreement -> {
                assertThat(agreement.getUserId()).isEqualTo(USER_ID);
                assertThat(agreement.getRecordedAt()).isEqualTo(RECORDED_AT);
                assertThat(agreement.getInputContext()).isEqualTo(AgreementInputContext.INDIVIDUAL);
                assertThat(agreement.getClientIp()).isEqualTo(CLIENT_IP);
                assertThat(agreement.getUserAgent()).isEqualTo(USER_AGENT);
              });
      assertThat(repository.findAll())
          .extracting(UserAgreement::getAgreementType, UserAgreement::getDocumentContent)
          .containsExactly(
              tuple(AgreementType.TERMS_OF_SERVICE, "이용약관 원문"),
              tuple(AgreementType.PRIVACY_COLLECTION_USE, "개인정보 원문"));
    }

    @Test
    @DisplayName("전체 선택으로 제출한 각 항목은 BULK 맥락으로 기록한다")
    void submit_whenBulkItemsAreSubmitted_recordsBulkContext() {
      service.submit(
          USER_ID, requiredSubmissions(AgreementInputContext.BULK), CLIENT_IP, USER_AGENT);

      assertThat(repository.findAll())
          .extracting(UserAgreement::getInputContext)
          .containsExactly(AgreementInputContext.BULK, AgreementInputContext.BULK);
    }

    @Test
    @DisplayName("필수 항목을 모두 동의하면 조회와 같은 충족 결과를 반환한다")
    void submit_whenAllRequiredItemsAreAgreed_returnsEligibleEvaluation() {
      AgreementEvaluation result =
          service.submit(
              USER_ID,
              requiredSubmissions(AgreementInputContext.INDIVIDUAL),
              CLIENT_IP,
              USER_AGENT);

      assertThat(result.eligible()).isTrue();
      assertThat(result.items()).filteredOn(Item::required).allMatch(Item::agreed);
      assertThat(item(result, AgreementType.TERMS_OF_SERVICE).recordedAt()).isEqualTo(RECORDED_AT);
      assertThat(item(result, AgreementType.MARKETING).agreed()).isFalse();
      assertThat(item(result, AgreementType.MARKETING).recordedAt()).isNull();
      assertThat(result).isEqualTo(service.getStatus(USER_ID));
    }

    @Test
    @DisplayName("같은 요청을 다시 제출하면 이력을 추가하고 판정 결과는 유지한다")
    void submit_whenSameRequestIsRetried_appendsEventsAndKeepsEvaluation() {
      List<AgreementSubmission> submissions = requiredSubmissions(AgreementInputContext.BULK);

      AgreementEvaluation first = service.submit(USER_ID, submissions, CLIENT_IP, USER_AGENT);
      AgreementEvaluation retried = service.submit(USER_ID, submissions, CLIENT_IP, USER_AGENT);

      assertThat(repository.findAll()).hasSize(4);
      assertThat(retried).isEqualTo(first);
    }

    @Test
    @DisplayName("동의 후 철회하면 기존 이력을 유지하고 최신 상태를 미동의로 반환한다")
    void submit_whenLatestActionIsRevoke_appendsRevokeAndReturnsNotEligible() {
      service.submit(
          USER_ID, requiredSubmissions(AgreementInputContext.INDIVIDUAL), CLIENT_IP, USER_AGENT);
      AgreementSubmission revoke =
          new AgreementSubmission(
              AgreementType.TERMS_OF_SERVICE,
              AgreementAction.REVOKE,
              "이용약관 원문",
              AgreementInputContext.INDIVIDUAL);

      AgreementEvaluation result = service.submit(USER_ID, List.of(revoke), CLIENT_IP, USER_AGENT);

      assertThat(repository.findAll()).hasSize(3);
      assertThat(result.eligible()).isFalse();
      assertThat(item(result, AgreementType.TERMS_OF_SERVICE).agreed()).isFalse();
      assertThat(item(result, AgreementType.TERMS_OF_SERVICE).recordedAt()).isEqualTo(RECORDED_AT);
    }

    @Test
    @DisplayName("빈 원문이 포함되면 어떤 이력도 저장하지 않는다")
    void submit_whenContentIsBlank_throwsExceptionWithoutSaving() {
      AgreementSubmission invalid =
          new AgreementSubmission(
              AgreementType.PRIVACY_COLLECTION_USE,
              AgreementAction.AGREE,
              " ",
              AgreementInputContext.INDIVIDUAL);

      assertThatThrownBy(
              () ->
                  service.submit(
                      USER_ID,
                      List.of(
                          requiredSubmissions(AgreementInputContext.INDIVIDUAL).getFirst(),
                          invalid),
                      CLIENT_IP,
                      USER_AGENT))
          .isInstanceOf(AgreementException.class)
          .extracting("exceptionCode")
          .isEqualTo(AgreementExceptionCode.AGREEMENT_CONTENT_EMPTY);
      assertThat(repository.findAll()).isEmpty();
    }
  }

  private List<AgreementSubmission> requiredSubmissions(AgreementInputContext inputContext) {
    return List.of(
        new AgreementSubmission(
            AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, "이용약관 원문", inputContext),
        new AgreementSubmission(
            AgreementType.PRIVACY_COLLECTION_USE, AgreementAction.AGREE, "개인정보 원문", inputContext));
  }

  private Item item(AgreementEvaluation evaluation, AgreementType type) {
    return evaluation.items().stream()
        .filter(item -> item.type() == type)
        .findFirst()
        .orElseThrow();
  }
}
