package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import java.util.ArrayList;
import java.util.List;

public class InMemoryAlcoholLookupSnapshotStore implements AlcoholLookupSnapshotStore {
  private List<AlcoholLookupItem> snapshot = new ArrayList<>();

  @Override
  public List<AlcoholLookupItem> findAll() {
    return List.copyOf(snapshot);
  }

  @Override
  public void replaceAll(List<AlcoholLookupItem> items) {
    snapshot = new ArrayList<>(items);
  }
}
