package app.bottlenote.alcohols.fixture;

import static app.bottlenote.shared.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

import app.bottlenote.core.alcohols.application.AlcoholFacade;
import app.bottlenote.shared.alcohols.exception.AlcoholException;
import app.bottlenote.shared.alcohols.payload.AlcoholSummaryItem;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FakeAlcoholFacade implements AlcoholFacade {

  private static final Logger log = LogManager.getLogger(FakeAlcoholFacade.class);

  private final Map<Long, AlcoholSummaryItem> alcoholDatabase = new ConcurrentHashMap<>();

  public FakeAlcoholFacade() {
    alcoholDatabase.put(
        1L,
        new AlcoholSummaryItem(
            1L,
            "위스키",
            "Whiskey",
            "위스키 카테고리",
            "Whiskey Category",
            "https://bottlenote.app/alcohol/1",
            false));
    alcoholDatabase.put(
        2L,
        new AlcoholSummaryItem(
            2L, "럼", "Rum", "럼 카테고리", "Rum Category", "https://bottlenote.app/alcohol/2", false));
    alcoholDatabase.put(
        3L,
        new AlcoholSummaryItem(
            3L,
            "보드카",
            "Vodka",
            "보드카 카테고리",
            "Vodka Category",
            "https://bottlenote.app/alcohol/3",
            false));
  }

  /**
   * Utility method to add alcohol data for testing purposes.
   *
   * @param alcoholSummaryItem Alcohol information to add.
   */
  public void addAlcohol(AlcoholSummaryItem alcoholSummaryItem) {
    Objects.requireNonNull(alcoholSummaryItem, "AlcoholSummaryItem cannot be null");
    alcoholDatabase.put(alcoholSummaryItem.alcoholId(), alcoholSummaryItem);
    log.debug("Added alcohol: {}", alcoholSummaryItem);
  }

  /**
   * Utility method to remove alcohol data by ID.
   *
   * @param alcoholId ID of the alcohol to remove.
   */
  public void removeAlcoholById(Long alcoholId) {
    alcoholDatabase.remove(alcoholId);
    log.debug("Removed alcohol with ID: {}", alcoholId);
  }

  /** Utility method to clear all alcohol data. */
  public void clearAlcoholDatabase() {
    alcoholDatabase.clear();
    log.debug("Cleared all alcohol data");
  }

  @Override
  public Pair<AlcoholSummaryItem, AlcoholSummaryItem> getAlcoholSummaryItemWithNext(
      Long alcoholId) {
    return alcoholDatabase.entrySet().stream()
        .filter(entry -> entry.getKey().equals(alcoholId))
        .map(
            entry -> {
              AlcoholSummaryItem current = entry.getValue();
              AlcoholSummaryItem next =
                  alcoholDatabase.values().stream()
                      .filter(item -> item.alcoholId() > current.alcoholId())
                      .findFirst()
                      .orElse(null);
              return Pair.of(current, next);
            })
        .findFirst()
        .orElse(Pair.of(null, null));
  }

  @Override
  public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long currentUserId) {
    AlcoholSummaryItem alcoholSummaryItem = alcoholDatabase.get(alcoholId);
    if (alcoholSummaryItem != null) {
      log.debug("Found AlcoholSummaryItem for ID {}: {}", alcoholId, alcoholSummaryItem);
      return Optional.of(alcoholSummaryItem);
    } else {
      log.debug("No AlcoholSummaryItem found for ID {}", alcoholId);
      return Optional.empty();
    }
  }

  @Override
  public Boolean existsByAlcoholId(Long alcoholId) {
    boolean exists = alcoholDatabase.containsKey(alcoholId);
    log.debug("Exists check for Alcohol ID {}: {}", alcoholId, exists);
    return exists;
  }

  @Override
  public void isValidAlcoholId(Long alcoholId) {
    if (!existsByAlcoholId(alcoholId)) {
      log.error("Alcohol ID {} not found", alcoholId);
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    }
  }

  @Override
  public Optional<String> findAlcoholImageUrlById(Long alcoholId) {
    AlcoholSummaryItem alcoholSummaryItem = alcoholDatabase.get(alcoholId);
    if (alcoholSummaryItem != null) {
      log.debug("Found image URL for Alcohol ID {}: {}", alcoholId, alcoholSummaryItem.imageUrl());
      return Optional.of(alcoholSummaryItem.imageUrl());
    } else {
      log.debug("No image URL found for Alcohol ID {}", alcoholId);
      return Optional.empty();
    }
  }
}
