package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import java.util.ArrayList;
import java.util.List;

public class InMemoryAlcoholLookupSnapshotStore implements AlcoholLookupSnapshotStore {
  private List<AlcoholLookupSnapshotItem> snapshot = new ArrayList<>();

  @Override
  public List<AlcoholLookupSnapshotItem> findAll() {
    return List.copyOf(snapshot);
  }

  @Override
  public void replaceAll(List<AlcoholLookupSnapshotItem> items) {
    snapshot = new ArrayList<>(items);
  }
}
