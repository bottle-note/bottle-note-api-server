package app.bottlenote.curation.dto.response;

import java.util.List;

// changedSpecIds: responseSpec이 실제로 바뀐 기존 스펙. 신규 생성 스펙은 큐레이션이 없어 제외한다.
public record CurationSpecSyncResponse(
    int createdCount, int updatedCount, List<Long> changedSpecIds) {

  // null을 빈 목록으로 뭉개면 재생성 누락이 "변경 없음"으로 위장된다. NPE로 즉시 드러나게 둔다.
  public CurationSpecSyncResponse {
    changedSpecIds = List.copyOf(changedSpecIds);
  }

  public int totalCount() {
    return createdCount + updatedCount;
  }

  public boolean hasChangedSpecs() {
    return !changedSpecIds.isEmpty();
  }
}
