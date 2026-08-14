package app.bottlenote.global.pagination;

import java.util.List;
import java.util.function.Function;

public record Pagination(boolean hasNext, String nextCursor) {

  public static <T> PageSlice<T> fromOverflow(
      List<T> fetched, int pageSize, Function<T, String> lastItemEncoder) {
    if (fetched == null || fetched.isEmpty()) {
      return new PageSlice<>(List.of(), new Pagination(false, null));
    }
    boolean hasNext = fetched.size() > pageSize;
    List<T> items = hasNext ? List.copyOf(fetched.subList(0, pageSize)) : List.copyOf(fetched);
    String nextCursor = hasNext ? lastItemEncoder.apply(items.get(items.size() - 1)) : null;
    return new PageSlice<>(items, new Pagination(hasNext, nextCursor));
  }

  public record PageSlice<T>(List<T> items, Pagination pagination) {
    public PageResponse<List<T>> toPageResponse() {
      return PageResponse.of(items, pagination);
    }
  }
}
