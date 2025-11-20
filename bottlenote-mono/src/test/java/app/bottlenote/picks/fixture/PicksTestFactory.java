package app.bottlenote.picks.fixture;

import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.Picks;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Picks 엔티티 테스트 팩토리
 *
 * <p>테스트에서 Picks 엔티티를 생성하고 영속화하는 헬퍼 클래스
 *
 * <p>철학: 1. 단일 책임: 엔티티 생성과 영속화만 담당 2. 격리: 모든 persist 메서드는 em.flush()를 호출하여 DB 반영 보장 3. 순수성:
 * Repository를 사용하지 않고 EntityManager만 사용 4. 명시성: 모든 파라미터와 반환값에 @NotNull/@Nullable 명시 5. 응집성: 다른 팩토리에
 * 의존하지 않음
 */
@Component
public class PicksTestFactory {

  @PersistenceContext private EntityManager em;

  /** 기본 Picks 생성 (alcoholId, userId 지정) */
  @Transactional
  @NotNull
  public Picks persistPicks(@NotNull Long alcoholId, @NotNull Long userId) {
    Picks picks = Picks.builder().alcoholId(alcoholId).userId(userId).build();

    em.persist(picks);
    em.flush();
    return picks;
  }

  /** Picks 생성 (alcoholId, userId, status 지정) */
  @Transactional
  @NotNull
  public Picks persistPicks(
      @NotNull Long alcoholId, @NotNull Long userId, @NotNull PicksStatus status) {
    Picks picks = Picks.builder().alcoholId(alcoholId).userId(userId).status(status).build();

    em.persist(picks);
    em.flush();
    return picks;
  }

  /** 빌더를 사용한 Picks 생성 */
  @Transactional
  @NotNull
  public Picks persistPicks(@NotNull Picks.PicksBuilder builder) {
    Picks tempPicks = builder.build();
    Picks.PicksBuilder finalBuilder = fillMissingPicksFields(tempPicks, builder);

    Picks picks = finalBuilder.build();
    em.persist(picks);
    em.flush();
    return picks;
  }

  /** 여러 Picks 생성 (한 사용자가 여러 술을 찜) */
  @Transactional
  @NotNull
  public List<Picks> persistMultiplePicksByUser(@NotNull Long userId, int count) {
    List<Picks> picksList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Long alcoholId = 1000L + i;
      picksList.add(persistPicks(alcoholId, userId));
    }
    return picksList;
  }

  /** 여러 Picks 생성 (여러 사용자가 한 술을 찜) */
  @Transactional
  @NotNull
  public List<Picks> persistMultiplePicksByAlcohol(@NotNull Long alcoholId, int count) {
    List<Picks> picksList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Long userId = 1000L + i;
      picksList.add(persistPicks(alcoholId, userId));
    }
    return picksList;
  }

  private Picks.PicksBuilder fillMissingPicksFields(Picks tempPicks, Picks.PicksBuilder builder) {
    if (tempPicks.getAlcoholId() == null) {
      throw new IllegalArgumentException("Picks 생성을 위해 alcoholId가 필요합니다.");
    }
    if (tempPicks.getUserId() == null) {
      throw new IllegalArgumentException("Picks 생성을 위해 userId가 필요합니다.");
    }
    return builder;
  }
}
