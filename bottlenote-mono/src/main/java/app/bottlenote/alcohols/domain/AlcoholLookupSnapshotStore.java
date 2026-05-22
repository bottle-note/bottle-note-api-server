package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import java.util.List;

public interface AlcoholLookupSnapshotStore {

  List<AlcoholLookupItem> findAll();

  void replaceAll(List<AlcoholLookupItem> items);
}
