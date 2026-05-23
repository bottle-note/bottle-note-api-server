package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import java.util.List;

public interface AlcoholLookupSnapshotStore {

  List<AlcoholLookupSnapshotItem> findAll();

  void replaceAll(List<AlcoholLookupSnapshotItem> items);
}
