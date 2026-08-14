package app.bottlenote.global.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("Pagination 공통 타입 단위 테스트")
class PaginationTest {

  @Test
  @DisplayName("size+1 목록이면 hasNext가 true이고 마지막 항목으로 nextCursor를 만든다")
  void fromOverflow_trims_and_encodes_last_item() {
    Pagination.PageSlice<Integer> slice =
        Pagination.fromOverflow(List.of(1, 2, 3, 4), 3, last -> "c-" + last);

    assertThat(slice.items()).containsExactly(1, 2, 3);
    assertThat(slice.pagination().hasNext()).isTrue();
    assertThat(slice.pagination().nextCursor()).isEqualTo("c-3");
    assertThat(slice.toPageResponse().content()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("조회 수가 size 이하면 hasNext가 false이고 nextCursor는 null이다")
  void fromOverflow_without_overflow_has_no_next() {
    Pagination.PageSlice<Integer> slice =
        Pagination.fromOverflow(List.of(1, 2), 3, last -> "c-" + last);

    assertThat(slice.items()).containsExactly(1, 2);
    assertThat(slice.pagination().hasNext()).isFalse();
    assertThat(slice.pagination().nextCursor()).isNull();
  }

  @Test
  @DisplayName("size가 없거나 1 미만이면 기본 10을 사용한다")
  void paginationRequest_defaults_size() {
    assertThat(new PaginationRequest(null, null).size()).isEqualTo(10);
    assertThat(new PaginationRequest("abc", 0).size()).isEqualTo(10);
    assertThat(new PaginationRequest("abc", 20).hasCursor()).isTrue();
  }
}
