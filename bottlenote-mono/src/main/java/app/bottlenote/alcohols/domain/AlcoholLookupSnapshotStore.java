package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.request.AlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import java.util.List;
import java.util.Optional;

public interface AlcoholLookupSnapshotStore {

  List<AlcoholLookupSnapshotItem> findAll();

  Optional<List<AlcoholLookupSnapshotItem>> findIndexed(AlcoholLookupRequest request);

  Optional<String> findVersion();

  void replaceAll(List<AlcoholLookupSnapshotItem> items);
}
