package app.bottlenote.global.pagination;

public record PageResponse<T>(T content, Pagination pagination) {

  public static <T> PageResponse<T> of(T content, Pagination pagination) {
    return new PageResponse<>(content, pagination);
  }
}
