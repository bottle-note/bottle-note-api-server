package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AdminAlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.global.data.response.GlobalResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAlcoholLookupService {
  private final AlcoholLookupSnapshotService snapshotService;

  @Transactional(readOnly = true)
  public GlobalResponse lookup(AdminAlcoholLookupRequest request) {
    List<AlcoholLookupItem> filtered =
        snapshotService.findFilteredItems(
            request.keyword(), request.categoryGroup(), request.regionId(), request.distilleryId());
    int from = Math.min(request.page() * request.size(), filtered.size());
    int to = Math.min(from + request.size(), filtered.size());
    return GlobalResponse.fromPage(
        new PageImpl<>(
            filtered.subList(from, to),
            PageRequest.of(request.page(), request.size()),
            filtered.size()));
  }
}
