package app.bottlenote.history.fixture;

import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.domain.UserHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * UserHistory 엔티티 테스트 팩토리
 *
 * <p>테스트에서 UserHistory 엔티티를 생성하고 영속화하는 헬퍼 클래스
 *
 * <p>철학: 1. 단일 책임: 엔티티 생성과 영속화만 담당 2. 격리: 모든 persist 메서드는 em.flush()를 호출하여 DB 반영 보장 3. 순수성:
 * Repository를 사용하지 않고 EntityManager만 사용 4. 명시성: 모든 파라미터와 반환값에 @NotNull/@Nullable 명시 5. 응집성: 다른 팩토리에
 * 의존하지 않음
 */
@Component
public class HistoryTestFactory {

  @PersistenceContext private EntityManager em;
  private final Random random = new Random();

  /** 기본 UserHistory 생성 (userId, eventType 지정) */
  @Transactional
  @NotNull
  public UserHistory persistUserHistory(@NotNull Long userId, @NotNull EventType eventType) {
    LocalDate now = LocalDate.now();
    UserHistory history =
        UserHistory.builder()
            .userId(userId)
            .eventCategory(eventType.getEventCategory())
            .eventType(eventType)
            .eventYear(String.valueOf(now.getYear()))
            .eventMonth(String.format("%02d", now.getMonthValue()))
            .build();

    em.persist(history);
    em.flush();
    return history;
  }

  /** UserHistory 생성 (userId, eventType, alcoholId 지정) */
  @Transactional
  @NotNull
  public UserHistory persistUserHistory(
      @NotNull Long userId, @NotNull EventType eventType, @NotNull Long alcoholId) {
    LocalDate now = LocalDate.now();
    UserHistory history =
        UserHistory.builder()
            .userId(userId)
            .eventCategory(eventType.getEventCategory())
            .eventType(eventType)
            .alcoholId(alcoholId)
            .eventYear(String.valueOf(now.getYear()))
            .eventMonth(String.format("%02d", now.getMonthValue()))
            .build();

    em.persist(history);
    em.flush();
    return history;
  }

  /** UserHistory 생성 (userId, eventType, alcoholId, content 지정) */
  @Transactional
  @NotNull
  public UserHistory persistUserHistory(
      @NotNull Long userId,
      @NotNull EventType eventType,
      @Nullable Long alcoholId,
      @NotNull String content) {
    LocalDate now = LocalDate.now();
    UserHistory history =
        UserHistory.builder()
            .userId(userId)
            .eventCategory(eventType.getEventCategory())
            .eventType(eventType)
            .alcoholId(alcoholId)
            .content(content)
            .eventYear(String.valueOf(now.getYear()))
            .eventMonth(String.format("%02d", now.getMonthValue()))
            .build();

    em.persist(history);
    em.flush();
    return history;
  }

  /** UserHistory 생성 (모든 필드 지정) */
  @Transactional
  @NotNull
  public UserHistory persistUserHistory(
      @NotNull Long userId,
      @NotNull EventType eventType,
      @Nullable Long alcoholId,
      @Nullable String redirectUrl,
      @Nullable String imageUrl,
      @Nullable String content) {
    LocalDate now = LocalDate.now();
    UserHistory history =
        UserHistory.builder()
            .userId(userId)
            .eventCategory(eventType.getEventCategory())
            .eventType(eventType)
            .alcoholId(alcoholId)
            .redirectUrl(redirectUrl)
            .imageUrl(imageUrl)
            .content(content)
            .eventYear(String.valueOf(now.getYear()))
            .eventMonth(String.format("%02d", now.getMonthValue()))
            .build();

    em.persist(history);
    em.flush();
    return history;
  }

  /** 빌더를 사용한 UserHistory 생성 */
  @Transactional
  @NotNull
  public UserHistory persistUserHistory(@NotNull UserHistory.UserHistoryBuilder builder) {
    UserHistory tempHistory = builder.build();
    UserHistory.UserHistoryBuilder finalBuilder = fillMissingHistoryFields(tempHistory, builder);

    UserHistory history = finalBuilder.build();
    em.persist(history);
    em.flush();
    return history;
  }

  /** 여러 UserHistory 생성 (같은 사용자의 여러 이벤트) */
  @Transactional
  @NotNull
  public List<UserHistory> persistMultipleUserHistories(@NotNull Long userId, int count) {
    List<UserHistory> histories = new ArrayList<>();
    EventType[] eventTypes = EventType.values();

    for (int i = 0; i < count; i++) {
      EventType eventType = eventTypes[i % eventTypes.length];
      histories.add(persistUserHistory(userId, eventType));
    }
    return histories;
  }

  /** 여러 UserHistory 생성 (같은 사용자, 같은 이벤트 타입) */
  @Transactional
  @NotNull
  public List<UserHistory> persistMultipleUserHistories(
      @NotNull Long userId, @NotNull EventType eventType, int count) {
    List<UserHistory> histories = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Long alcoholId = 1000L + i;
      histories.add(persistUserHistory(userId, eventType, alcoholId));
    }
    return histories;
  }

  /** 다이나믹 메시지가 포함된 UserHistory 생성 */
  @Transactional
  @NotNull
  public UserHistory persistUserHistoryWithDynamicMessage(
      @NotNull Long userId,
      @NotNull EventType eventType,
      @NotNull Map<String, String> dynamicMessage) {
    LocalDate now = LocalDate.now();
    UserHistory history =
        UserHistory.builder()
            .userId(userId)
            .eventCategory(eventType.getEventCategory())
            .eventType(eventType)
            .dynamicMessage(dynamicMessage)
            .eventYear(String.valueOf(now.getYear()))
            .eventMonth(String.format("%02d", now.getMonthValue()))
            .build();

    em.persist(history);
    em.flush();
    return history;
  }

  private String generateRandomSuffix() {
    return String.valueOf(random.nextInt(10000));
  }

  private UserHistory.UserHistoryBuilder fillMissingHistoryFields(
      UserHistory tempHistory, UserHistory.UserHistoryBuilder builder) {
    if (tempHistory.getUserId() == null) {
      throw new IllegalArgumentException("UserHistory 생성을 위해 userId가 필요합니다.");
    }
    if (tempHistory.getEventCategory() == null) {
      throw new IllegalArgumentException("UserHistory 생성을 위해 eventCategory가 필요합니다.");
    }
    if (tempHistory.getEventType() == null) {
      throw new IllegalArgumentException("UserHistory 생성을 위해 eventType가 필요합니다.");
    }
    if (tempHistory.getEventYear() == null) {
      LocalDate now = LocalDate.now();
      builder.eventYear(String.valueOf(now.getYear()));
    }
    if (tempHistory.getEventMonth() == null) {
      LocalDate now = LocalDate.now();
      builder.eventMonth(String.format("%02d", now.getMonthValue()));
    }
    return builder;
  }
}
