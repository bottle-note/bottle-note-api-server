package app.bottlenote.agreement.fixture;

import app.bottlenote.agreement.constant.AgreementType;
import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.domain.UserAgreementRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryUserAgreementRepository implements UserAgreementRepository {

  private static final Comparator<UserAgreement> LATEST_FIRST =
      Comparator.comparing(UserAgreement::getId).reversed();

  private final AtomicLong idGenerator = new AtomicLong(1L);
  private final List<UserAgreement> database = new ArrayList<>();

  @Override
  public UserAgreement save(UserAgreement userAgreement) {
    Objects.requireNonNull(userAgreement, "userAgreement는 null일 수 없습니다.");
    if (userAgreement.getId() != null) {
      throw new IllegalArgumentException("이미 저장된 동의 이력은 다시 저장할 수 없습니다.");
    }
    ReflectionTestUtils.setField(userAgreement, "id", idGenerator.getAndIncrement());
    database.add(userAgreement);
    return userAgreement;
  }

  @Override
  public Optional<UserAgreement> findFirstByUserIdAndAgreementTypeOrderByIdDesc(
      Long userId, AgreementType agreementType) {
    return database.stream()
        .filter(userAgreement -> userAgreement.getUserId().equals(userId))
        .filter(userAgreement -> userAgreement.getAgreementType() == agreementType)
        .sorted(LATEST_FIRST)
        .findFirst();
  }

  public List<UserAgreement> findAll() {
    return List.copyOf(database);
  }
}
