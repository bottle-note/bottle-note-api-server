package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.dto.request.AlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAlcoholLookupSnapshotStore implements AlcoholLookupSnapshotStore {
  private List<AlcoholLookupSnapshotItem> snapshot = new ArrayList<>();
  private final AtomicLong version = new AtomicLong();
  private int findAllCount;
  private int findIndexedCount;

  @Override
  public List<AlcoholLookupSnapshotItem> findAll() {
    findAllCount++;
    return List.copyOf(snapshot);
  }

  @Override
  public Optional<List<AlcoholLookupSnapshotItem>> findIndexed(AlcoholLookupRequest request) {
    findIndexedCount++;
    boolean hasIndexCondition =
        (request.keyword() != null && !request.keyword().isBlank())
            || request.categoryGroup() != null
            || request.regionId() != null
            || request.distilleryId() != null;
    if (!hasIndexCondition || snapshot.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        snapshot.stream()
            .filter(item -> matchesKeyword(item, request.keyword()))
            .filter(
                item ->
                    request.categoryGroup() == null
                        || request.categoryGroup() == item.categoryGroup())
            .filter(
                item -> request.regionId() == null || request.regionId().equals(item.regionId()))
            .filter(
                item ->
                    request.distilleryId() == null
                        || request.distilleryId().equals(item.distilleryId()))
            .toList());
  }

  @Override
  public Optional<String> findVersion() {
    if (version.get() == 0L) {
      return Optional.empty();
    }
    return Optional.of(Long.toString(version.get()));
  }

  @Override
  public void replaceAll(List<AlcoholLookupSnapshotItem> items) {
    snapshot = new ArrayList<>(items);
    version.incrementAndGet();
  }

  public int findAllCount() {
    return findAllCount;
  }

  public int findIndexedCount() {
    return findIndexedCount;
  }

  private boolean matchesKeyword(AlcoholLookupSnapshotItem item, String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return true;
    }
    for (String token : keyword.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
      if (!token.isBlank() && !item.normalizedSearchText().contains(token)) {
        return false;
      }
    }
    return true;
  }
}
