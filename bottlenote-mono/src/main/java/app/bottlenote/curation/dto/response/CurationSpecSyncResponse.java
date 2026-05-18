package app.bottlenote.curation.dto.response;

public record CurationSpecSyncResponse(int createdCount, int updatedCount) {

  public int totalCount() {
    return createdCount + updatedCount;
  }
}
