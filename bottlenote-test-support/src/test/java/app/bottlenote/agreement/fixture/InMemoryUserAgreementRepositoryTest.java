package app.bottlenote.agreement.fixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.UserAgreement;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("InMemoryUserAgreementRepository 단위 테스트")
class InMemoryUserAgreementRepositoryTest {

  private static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 8, 1, 0, 0);

  private InMemoryUserAgreementRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryUserAgreementRepository();
  }

  @Test
  @DisplayName("동의 이력을 저장할 때 기존 이력을 유지하고 새 ID를 부여한다")
  void save_whenMultipleEvents_appendsWithGeneratedIds() {
    UserAgreement first = agreement(1L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT);
    UserAgreement second = agreement(1L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT.plusDays(1));

    repository.save(first);
    repository.save(second);

    assertThat(repository.findAll()).containsExactly(first, second);
    assertThat(first.getId()).isEqualTo(1L);
    assertThat(second.getId()).isEqualTo(2L);
  }

  @Test
  @DisplayName("이미 저장된 이력을 다시 저장할 때 거부한다")
  void save_whenEventAlreadyPersisted_throwsException() {
    UserAgreement agreement = agreement(1L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT);
    repository.save(agreement);

    assertThatThrownBy(() -> repository.save(agreement))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(repository.findAll()).containsExactly(agreement);
  }

  @Test
  @DisplayName("기록 시각과 관계없이 더 큰 ID의 이력을 반환한다")
  void findLatest_whenRecordedAtDiffers_returnsHighestId() {
    UserAgreement first =
        agreement(1L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT.plusSeconds(1));
    UserAgreement second = agreement(1L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT);
    repository.save(first);
    repository.save(second);

    assertThat(latest(1L, AgreementType.TERMS_OF_SERVICE)).contains(second);
  }

  @Test
  @DisplayName("기록 시각이 같아도 더 큰 ID의 이력을 반환한다")
  void findLatest_whenRecordedAtTies_returnsHighestId() {
    UserAgreement first = agreement(1L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT);
    UserAgreement second = agreement(1L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT);
    repository.save(first);
    repository.save(second);

    assertThat(latest(1L, AgreementType.TERMS_OF_SERVICE)).contains(second);
  }

  @Test
  @DisplayName("조회할 때 사용자와 동의 유형을 모두 격리한다")
  void findLatest_whenUsersAndTypesDiffer_returnsOnlyMatchingEvent() {
    UserAgreement expected = agreement(1L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT);
    repository.save(expected);
    repository.save(agreement(2L, AgreementType.TERMS_OF_SERVICE, RECORDED_AT.plusDays(1)));
    repository.save(agreement(1L, AgreementType.PRIVACY_COLLECTION_USE, RECORDED_AT.plusDays(1)));

    assertThat(latest(1L, AgreementType.TERMS_OF_SERVICE)).contains(expected);
  }

  private Optional<UserAgreement> latest(Long userId, AgreementType agreementType) {
    return repository.findFirstByUserIdAndAgreementTypeOrderByIdDesc(userId, agreementType);
  }

  private UserAgreement agreement(
      Long userId, AgreementType agreementType, LocalDateTime recordedAt) {
    return UserAgreement.create(
        userId,
        agreementType,
        AgreementAction.AGREE,
        "document",
        recordedAt,
        AgreementInputContext.INDIVIDUAL,
        "127.0.0.1",
        "test-agent");
  }
}
