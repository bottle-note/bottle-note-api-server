package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupListResponse;
import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.CursorKeys;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlcoholLookupService {
  private final AlcoholLookupSnapshotService snapshotService;
  private final HmacCursorCodec cursorCodec;

  @Transactional(readOnly = true)
  public KeysetPageResponse<AlcoholLookupListResponse> lookup(AlcoholLookupRequest request) {
    String context = lookupContext(request);
    Long lastId = null;
    if (request.cursor() != null) {
      CursorClaims claims = cursorCodec.verify(request.cursor(), context);
      lastId = CursorKeys.requireLong(claims, "id");
    }
    Long seekAfter = lastId;
    List<AlcoholLookupItem> fetched =
        snapshotService
            .findFilteredItems(
                request.keyword(),
                request.categoryGroup(),
                request.regionId(),
                request.distilleryId())
            .stream()
            .filter(item -> seekAfter == null || item.alcoholId() > seekAfter)
            .limit(request.size() + 1L)
            .toList();
    KeysetPagination.PageSlice<AlcoholLookupItem> slice =
        KeysetPagination.fromOverflow(
            fetched,
            request.size(),
            item ->
                cursorCodec.encode(
                    context, java.util.Map.of("id", String.valueOf(item.alcoholId()))));
    return KeysetPageResponse.of(new AlcoholLookupListResponse(slice.items()), slice.pagination());
  }

  @Transactional(readOnly = true)
  public AlcoholLookupSyncResult syncSnapshot() {
    return snapshotService.syncSnapshot();
  }

  private static String lookupContext(AlcoholLookupRequest request) {
    return "alcohol.lookup:"
        + request.keyword()
        + ":"
        + request.category()
        + ":"
        + request.regionId()
        + ":"
        + request.distilleryId();
  }

  public record AlcoholLookupSyncResult(int count, boolean changed) {

    public static AlcoholLookupSyncResult changed(int count) {
      return new AlcoholLookupSyncResult(count, true);
    }

    public static AlcoholLookupSyncResult unchanged(int count) {
      return new AlcoholLookupSyncResult(count, false);
    }
  }
}
