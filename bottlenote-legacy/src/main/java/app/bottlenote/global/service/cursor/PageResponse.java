package app.bottlenote.global.service.cursor;

public record PageResponse<T>(T content, CursorPageable cursorPageable) {
  public static <T> PageResponse<T> of(T content, CursorPageable cursorPageable) {
    return new PageResponse<>(content, cursorPageable);
  }
}
