package app.bottlenote.global.pagination;

public record PaginationRequest(String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 10;

  public static PaginationRequest of(String cursor, Integer size, int defaultSize, int maxSize) {
    int resolvedDefault = Math.max(defaultSize, 1);
    int resolvedMax = Math.max(maxSize, resolvedDefault);
    int resolved = (size == null || size < 1) ? resolvedDefault : Math.min(size, resolvedMax);
    String normalized = cursor == null || cursor.isBlank() ? null : cursor;
    return new PaginationRequest(normalized, resolved);
  }

  public PaginationRequest {
    size = (size == null || size < 1) ? DEFAULT_SIZE : size;
    cursor = cursor == null || cursor.isBlank() ? null : cursor;
  }

  public boolean hasCursor() {
    return cursor != null;
  }
}
