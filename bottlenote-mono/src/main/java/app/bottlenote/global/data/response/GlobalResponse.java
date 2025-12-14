package app.bottlenote.global.data.response;

import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;
import static java.util.Collections.emptyList;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.meta.MetaInfos;
import app.bottlenote.global.service.meta.MetaService;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 전역 응답 객체 <br>
 * 성공 / 실패 / 에러에 대한 공통 응답 객체를 생성한다. <br>
 * 모든 응답 값은 해당 객체를 통해 생성한다. <br>
 */
@Getter
@Slf4j
public class GlobalResponse {
  private final Boolean success;
  private final Integer code;
  private final Object data;
  private final Object errors;
  private final Map<String, Object> meta;

  @Builder
  private GlobalResponse(
      Boolean success, Integer code, Object data, Object errors, Map<String, Object> meta) {
    this.success = success;
    this.code = code;
    this.data = data;
    this.errors = errors;
    this.meta = meta;
  }

  @JsonCreator
  public GlobalResponse(
      @JsonProperty("success") boolean success,
      @JsonProperty("code") int code,
      @JsonProperty("data") Object data,
      @JsonProperty("errors") List<String> errors,
      @JsonProperty("meta") Map<String, Object> meta) {
    this.success = success;
    this.code = code;
    this.data = data;
    this.errors = errors;
    this.meta = meta;
  }

  public static ResponseEntity<?> ok(Object data) {
    return ResponseEntity.ok(success(data));
  }

  public static <T, P> ResponseEntity<?> ok(
      Pair<Long, CursorResponse<T>> pair, P searchParameters) {
    Long totalCount = pair.getLeft();
    CursorResponse<T> items = pair.getRight();
    CollectionResponse<T> response = CollectionResponse.of(totalCount, items);
    MetaInfos metaInfos = MetaService.createMetaInfo();
    metaInfos.add("pageable", items.pageable());
    metaInfos.add("searchParameters", searchParameters);
    return ResponseEntity.ok(success(response, metaInfos));
  }

  public static ResponseEntity<?> ok(Object data, MetaInfos meta) {
    return ResponseEntity.ok(success(data, meta));
  }

  public static <T> ResponseEntity<?> ok(Page<T> page) {
    if (page == null) {
      return ResponseEntity.ok(success(emptyList()));
    }
    return ResponseEntity.ok(
        GlobalResponse.builder()
            .success(true)
            .code(200)
            .errors(emptyList())
            .data(page.getContent())
            .meta(
                createMetaInfo()
                    .add("page", page.getNumber())
                    .add("size", page.getSize())
                    .add("totalElements", page.getTotalElements())
                    .add("totalPages", page.getTotalPages())
                    .add("hasNext", page.hasNext())
                    .getMetaInfos())
            .build());
  }

  public static GlobalResponse success(Object data) {
    return GlobalResponse.builder()
        .success(true)
        .code(200)
        .errors(emptyList())
        .meta(createMetaInfo().getMetaInfos())
        .data(data)
        .build();
  }

  public static GlobalResponse success(Object data, MetaInfos meta) {
    return GlobalResponse.builder()
        .success(true)
        .code(200)
        .errors(emptyList())
        .meta(meta.getMetaInfos())
        .data(data)
        .build();
  }

  public static GlobalResponse fail(Object errors) {
    return GlobalResponse.builder()
        .success(false)
        .code(400)
        .data(emptyList())
        .errors(errors)
        .meta(createMetaInfo().getMetaInfos())
        .build();
  }

  public static GlobalResponse error(Integer code, Object errors) {
    return GlobalResponse.builder()
        .success(false)
        .code(code)
        .data(emptyList())
        .errors(errors)
        .meta(createMetaInfo().getMetaInfos())
        .build();
  }

  public static ResponseEntity<?> error(Error error) {
    GlobalResponse response =
        GlobalResponse.builder()
            .success(false)
            .code(error.code().getHttpStatus().value())
            .data(emptyList())
            .errors(List.of(error))
            .meta(createMetaInfo().getMetaInfos())
            .build();
    return new ResponseEntity<>(response, error.code().getHttpStatus());
  }

  public static ResponseEntity<?> error(AbstractCustomException exception) {

    Error error = Error.of(exception.getExceptionCode());

    GlobalResponse response =
        GlobalResponse.builder()
            .success(false)
            .code(exception.getExceptionCode().getHttpStatus().value())
            .data(emptyList())
            .errors(List.of(error))
            .meta(createMetaInfo().getMetaInfos())
            .build();

    return new ResponseEntity<>(response, exception.getExceptionCode().getHttpStatus());
  }

  public static ResponseEntity<?> error(Set<Error> errorSet) {
    GlobalResponse response =
        GlobalResponse.builder()
            .success(false)
            .code(HttpStatus.BAD_REQUEST.value())
            .data(emptyList())
            .errors(errorSet.stream().toList())
            .meta(createMetaInfo().getMetaInfos())
            .build();
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }
}
