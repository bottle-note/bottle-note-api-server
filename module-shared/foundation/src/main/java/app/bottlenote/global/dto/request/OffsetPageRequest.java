package app.bottlenote.global.dto.request;

public record OffsetPageRequest(int page, int size) {
  public static OffsetPageRequest of(Integer page, Integer size, int defaultSize, int maxSize) {
    return new OffsetPageRequest(
        page == null || page < 0 ? 0 : page,
        size == null || size < 1 ? defaultSize : Math.min(size, maxSize));
  }
}
