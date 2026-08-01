package app.bottlenote.agreement.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.domain.UserAgreementRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] 사용자 동의 저장소")
class UserAgreementRepositoryIntegrationTest extends IntegrationTestSupport {

  private static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 8, 1, 12, 0);

  @Autowired private UserAgreementRepository userAgreementRepository;
  @Autowired private UserTestFactory userTestFactory;
  @PersistenceContext private EntityManager entityManager;

  @Test
  @DisplayName("동의 이력을 저장할 때 V6 스키마의 모든 값을 보존한다")
  void save_whenAgreementCreated_preservesAllValues() {
    User user = userTestFactory.persistUser();
    UserAgreement agreement =
        createAgreement(
            user.getId(), AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, RECORDED_AT);

    UserAgreement saved = userAgreementRepository.save(agreement);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUserId()).isEqualTo(user.getId());
    assertThat(saved.getAgreementType()).isEqualTo(AgreementType.TERMS_OF_SERVICE);
    assertThat(saved.getAction()).isEqualTo(AgreementAction.AGREE);
    assertThat(saved.getDocumentContent()).isEqualTo("약관 원문");
    assertThat(saved.getRecordedAt()).isEqualTo(RECORDED_AT);
    assertThat(saved.getInputContext()).isEqualTo(AgreementInputContext.INDIVIDUAL);
    assertThat(saved.getClientIp()).isEqualTo("203.0.113.10");
    assertThat(saved.getUserAgent()).isEqualTo("BottleNote/Test");
  }

  @Test
  @DisplayName("기록 시각이 다를 때 가장 최근 동의 이력을 조회한다")
  void findLatest_whenRecordedAtDiffers_returnsNewestRecordedAt() {
    User user = userTestFactory.persistUser();
    UserAgreement newest =
        userAgreementRepository.save(
            createAgreement(
                user.getId(), AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, RECORDED_AT));
    userAgreementRepository.save(
        createAgreement(
            user.getId(),
            AgreementType.TERMS_OF_SERVICE,
            AgreementAction.REVOKE,
            RECORDED_AT.minusMinutes(1)));

    UserAgreement result =
        userAgreementRepository
            .findFirstByUserIdAndAgreementTypeOrderByRecordedAtDescIdDesc(
                user.getId(), AgreementType.TERMS_OF_SERVICE)
            .orElseThrow();

    assertThat(result.getId()).isEqualTo(newest.getId());
  }

  @Test
  @DisplayName("기록 시각이 같을 때 ID가 큰 동의 이력을 조회한다")
  void findLatest_whenRecordedAtTies_returnsHighestId() {
    User user = userTestFactory.persistUser();
    userAgreementRepository.save(
        createAgreement(
            user.getId(), AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, RECORDED_AT));
    UserAgreement latest =
        userAgreementRepository.save(
            createAgreement(
                user.getId(), AgreementType.TERMS_OF_SERVICE, AgreementAction.REVOKE, RECORDED_AT));

    UserAgreement result =
        userAgreementRepository
            .findFirstByUserIdAndAgreementTypeOrderByRecordedAtDescIdDesc(
                user.getId(), AgreementType.TERMS_OF_SERVICE)
            .orElseThrow();

    assertThat(result.getId()).isEqualTo(latest.getId());
    assertThat(result.getAction()).isEqualTo(AgreementAction.REVOKE);
  }

  @Test
  @DisplayName("최신 이력을 조회할 때 사용자와 동의 유형을 구분한다")
  void findLatest_whenUsersAndTypesDiffer_returnsMatchingAgreement() {
    User targetUser = userTestFactory.persistUser();
    User otherUser = userTestFactory.persistUser();
    UserAgreement expected =
        userAgreementRepository.save(
            createAgreement(
                targetUser.getId(),
                AgreementType.TERMS_OF_SERVICE,
                AgreementAction.AGREE,
                RECORDED_AT));
    userAgreementRepository.save(
        createAgreement(
            targetUser.getId(),
            AgreementType.PRIVACY_COLLECTION_USE,
            AgreementAction.REVOKE,
            RECORDED_AT.plusMinutes(1)));
    userAgreementRepository.save(
        createAgreement(
            otherUser.getId(),
            AgreementType.TERMS_OF_SERVICE,
            AgreementAction.REVOKE,
            RECORDED_AT.plusMinutes(2)));

    UserAgreement result =
        userAgreementRepository
            .findFirstByUserIdAndAgreementTypeOrderByRecordedAtDescIdDesc(
                targetUser.getId(), AgreementType.TERMS_OF_SERVICE)
            .orElseThrow();

    assertThat(result.getId()).isEqualTo(expected.getId());
  }

  @Test
  @DisplayName("새 의사표시를 저장할 때 기존 동의 이력을 유지한다")
  void save_whenActionChanges_appendsAgreementHistory() {
    User user = userTestFactory.persistUser();
    userAgreementRepository.save(
        createAgreement(
            user.getId(), AgreementType.TERMS_OF_SERVICE, AgreementAction.AGREE, RECORDED_AT));
    userAgreementRepository.save(
        createAgreement(
            user.getId(),
            AgreementType.TERMS_OF_SERVICE,
            AgreementAction.REVOKE,
            RECORDED_AT.plusMinutes(1)));

    Long count =
        entityManager
            .createQuery(
                "select count(ua) from userAgreement ua where ua.userId = :userId", Long.class)
            .setParameter("userId", user.getId())
            .getSingleResult();

    assertThat(count).isEqualTo(2L);
  }

  private UserAgreement createAgreement(
      Long userId, AgreementType agreementType, AgreementAction action, LocalDateTime recordedAt) {
    return UserAgreement.create(
        userId,
        agreementType,
        action,
        "약관 원문",
        recordedAt,
        AgreementInputContext.INDIVIDUAL,
        "203.0.113.10",
        "BottleNote/Test");
  }
}
