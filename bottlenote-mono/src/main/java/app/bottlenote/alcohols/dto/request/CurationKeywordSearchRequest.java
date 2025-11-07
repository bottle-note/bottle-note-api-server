package app.bottlenote.alcohols.dto.request;

public record CurationKeywordSearchRequest(
    String keyword, Long alcoholId, Long cursor, Long pageSize) {
  public CurationKeywordSearchRequest {
    if (cursor == null) {
      cursor = 0L;
    }
    if (pageSize == null) {
      pageSize = 10L;
    }
  }

  public static CurationKeywordSearchRequest of(
      String keyword, Long alcoholId, Long cursor, Long pageSize) {
    return new CurationKeywordSearchRequest(keyword, alcoholId, cursor, pageSize);
  }
}
