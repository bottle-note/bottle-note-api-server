package app.bottlenote.global.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("키셋 페이지네이션 공통 타입 단위 테스트")
class KeysetPaginationTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  @DisplayName("size+1 목록이면 hasNext가 true이고 마지막 항목으로 nextCursor를 만든다")
  void fromOverflow_trims_and_encodes_last_item() {
    KeysetPagination.PageSlice<Integer> slice =
        KeysetPagination.fromOverflow(List.of(1, 2, 3, 4), 3, last -> "c-" + last);

    assertThat(slice.items()).containsExactly(1, 2, 3);
    assertThat(slice.pagination().hasNext()).isTrue();
    assertThat(slice.pagination().nextCursor()).isEqualTo("c-3");
    assertThat(slice.toPageResponse().content()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("조회 수가 size 이하면 hasNext가 false이고 nextCursor는 null이다")
  void fromOverflow_without_overflow_has_no_next() {
    KeysetPagination.PageSlice<Integer> slice =
        KeysetPagination.fromOverflow(List.of(1, 2), 3, last -> "c-" + last);

    assertThat(slice.items()).containsExactly(1, 2);
    assertThat(slice.pagination().hasNext()).isFalse();
    assertThat(slice.pagination().nextCursor()).isNull();
  }

  @Test
  @DisplayName("키셋 페이지 응답 JSON은 기존 content와 pagination 필드 계약을 유지한다")
  void keysetPageResponse_preserves_json_contract() throws JsonProcessingException {
    KeysetPageResponse<List<String>> response =
        KeysetPageResponse.of(List.of("first", "second"), new KeysetPagination(true, "cursor-2"));

    assertThat(OBJECT_MAPPER.writeValueAsString(response))
        .isEqualTo(
            "{\"content\":[\"first\",\"second\"],\"pagination\":{\"hasNext\":true,\"nextCursor\":\"cursor-2\"}}");
  }

  @Test
  @DisplayName("size가 없거나 1 미만이면 기본 10을 사용한다")
  void keysetPageRequest_defaults_size() {
    assertThat(new KeysetPageRequest(null, null).size()).isEqualTo(10);
    assertThat(new KeysetPageRequest("abc", 0).size()).isEqualTo(10);
    assertThat(new KeysetPageRequest("abc", 20).hasCursor()).isTrue();
  }

  @Test
  @DisplayName("API별 기본값과 상한을 적용한다")
  void keysetPageRequest_of_uses_default_and_max() {
    assertThat(KeysetPageRequest.of(null, null, 50, 100).size()).isEqualTo(50);
    assertThat(KeysetPageRequest.of("c", 200, 20, 100).size()).isEqualTo(100);
    assertThat(KeysetPageRequest.of("  ", 3, 20, 100).hasCursor()).isFalse();
  }

  @Test
  @DisplayName("pageSize가 1 미만이면 예외를 던진다")
  void fromOverflow_rejects_non_positive_size() {
    assertThatThrownBy(() -> KeysetPagination.fromOverflow(List.of(1), 0, last -> "c"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
