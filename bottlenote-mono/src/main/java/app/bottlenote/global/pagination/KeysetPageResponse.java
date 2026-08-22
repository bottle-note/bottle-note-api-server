package app.bottlenote.global.pagination;

public record KeysetPageResponse<T>(T content, KeysetPagination pagination) {

  public static <T> KeysetPageResponse<T> of(T content, KeysetPagination pagination) {
    return new KeysetPageResponse<>(content, pagination);
  }
}
