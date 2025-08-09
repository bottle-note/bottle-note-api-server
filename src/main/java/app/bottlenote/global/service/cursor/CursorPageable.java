package app.bottlenote.global.service.cursor;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(of = {"currentCursor", "cursor", "pageSize", "hasNext"})
public class CursorPageable {
  private final Long currentCursor;
  private final Long cursor;
  private final Long pageSize;
  private final Boolean hasNext;

  /**
   * Instantiates a new Cursor pageable.
   *
   * @param currentCursor 현재 페이지의 커서 위치 ex) 최초 조회 시 0
   * @param cursor 다음 페이지를 위한 커서 위치 ex) 최초 조회 시 currentCursor + pageSize +1
   * @param pageSize 페이지당 항목 조회할 갯수
   * @param hasNext 다음 페이지 존재 여부
   */
  @Builder
  public CursorPageable(Long currentCursor, Long cursor, Long pageSize, Boolean hasNext) {
    this.currentCursor = currentCursor;
    this.cursor = cursor;
    this.pageSize = pageSize;
    this.hasNext = hasNext;
  }

  /**
   * 컬렉션과 현재 커서, 페이지 크기를 기반으로 CursorPageable 생성 추가 항목을 조회했을 경우 hasNext를 계산하고 추가 항목을 제거
   *
   * @param items 조회된 항목 목록 (pageSize + 1 개의 항목이 예상됨)
   * @param currentCursor 현재 커서 위치
   * @param pageSize 페이지 크기
   * @param <T> 항목의 타입
   * @return 생성된 CursorPageable 객체
   */
  public static <T> CursorPageable of(List<T> items, Long currentCursor, Long pageSize) {
    boolean hasNext = items.size() > pageSize;

    // 결과 리스트 생성 (원본 리스트를 수정하지 않음)
    List<T> result = hasNext ? items.subList(0, items.size() - 1) : items;

    return CursorPageable.builder()
        .currentCursor(currentCursor)
        .cursor(currentCursor + pageSize)
        .pageSize(pageSize)
        .hasNext(hasNext)
        .build();
  }

  /** Integer 타입의 pageSize를 위한 오버로딩 메소드 */
  public static <T> CursorPageable of(List<T> items, Long currentCursor, Integer pageSize) {
    return of(items, currentCursor, pageSize.longValue());
  }
}
