package app.bottlenote.global.pagination;

public record PaginationRequest(String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 10;

  public PaginationRequest {
    size = (size == null || size < 1) ? DEFAULT_SIZE : size;
  }

  public boolean hasCursor() {
    return cursor != null && !cursor.isBlank();
  }
}
