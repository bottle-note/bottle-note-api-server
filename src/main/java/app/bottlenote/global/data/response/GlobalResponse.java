package app.bottlenote.global.data.response;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import app.bottlenote.global.service.meta.MetaInfos;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;
import static java.util.Collections.emptyList;

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
	private GlobalResponse(Boolean success, Integer code, Object data, Object errors, Map<String, Object> meta) {
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
		@JsonProperty("meta") Map<String, Object> meta
	) {
		this.success = success;
		this.code = code;
		this.data = data;
		this.errors = errors;
		this.meta = meta;
	}

	public static ResponseEntity<?> ok(Object data) {
		return ResponseEntity.ok(success(data));
	}

	public static ResponseEntity<?> ok(Object data, MetaInfos meta) {
		return ResponseEntity.ok(success(data, meta));
	}

	/**
	 * 성공한 경우의 공통 응답 객체를 생성한다.
	 *
	 * @param data - 응답 데이터
	 * @return the global response
	 */
	public static GlobalResponse success(Object data) {
		return GlobalResponse.builder()
			.success(true)
			.code(200)
			.errors(emptyList())
			.meta(createMetaInfo().getMetaInfos())
			.data(data)
			.build();
	}

	/**
	 * 성공한 경우의 공통 응답 객체를 생성한다.
	 * 추가적인 메타 정보 ( 페이지값, 서버 요청 가능 횟수등 )를 추가적으로  포함한다.
	 *
	 * @param data the data
	 * @param meta the meta
	 * @return the global response
	 */
	public static GlobalResponse success(Object data, MetaInfos meta) {
		return GlobalResponse.builder()
			.success(true)
			.code(200)
			.errors(emptyList())
			.meta(meta.getMetaInfos())
			.data(data)
			.build();
	}

	/**
	 * 실패한 경우의 공통 응답 객체를 생성한다.
	 *
	 * @param errors 에러 메시지들
	 * @return the global response
	 */
	public static GlobalResponse fail(Object errors) {
		return GlobalResponse.builder()
			.success(false)
			.code(400)
			.data(emptyList())
			.errors(errors)
			.meta(createMetaInfo().getMetaInfos())
			.build();
	}

	/**
	 * 실패한 경우의 공통 응답 객체를 생성한다.
	 *
	 * @param code   http 상태값들
	 * @param errors 에러 메시지들
	 * @return the global response
	 */
	public static GlobalResponse fail(Integer code, Object errors) {
		return GlobalResponse.builder()
			.success(false)
			.code(code)
			.data(emptyList())
			.errors(errors)
			.meta(createMetaInfo().getMetaInfos())
			.build();
	}

	/**
	 * 실패한 경우의 공통 응답 객체를 생성한다.
	 *
	 * @param code   http 상태값들
	 * @param errors 에러 메시지들
	 * @param meta   추가적인 메타 정보 ( 실패로 인한 재시도 시 필요한 정보  )를 추가적으로 요청
	 * @return the global response
	 */
	public static GlobalResponse fail(Integer code, Object errors, MetaInfos meta) {
		return GlobalResponse.builder()
			.success(false)
			.code(code)
			.data(emptyList())
			.errors(errors)
			.meta(meta.getMetaInfos())
			.build();
	}

	/**
	 * 에러가 발생한 경우의 공통 응답 객체를 생성한다.
	 *
	 * @param code   the code
	 * @param errors the errors
	 * @return the global response
	 */
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
		GlobalResponse response = GlobalResponse.builder()
			.success(false)
			.code(error.code().getHttpStatus().value())
			.data(emptyList())
			.errors(error)
			.meta(createMetaInfo().getMetaInfos())
			.build();
		return new ResponseEntity<>(response, error.code().getHttpStatus());
	}

	public static ResponseEntity<?> error(AbstractCustomException exception) {
		GlobalResponse response = GlobalResponse.builder()
			.success(false)
			.code(exception.getExceptionCode().getHttpStatus().value())
			.data(emptyList())
			.errors(List.of(exception.getExceptionCode().getMessage()))
			.meta(createMetaInfo().getMetaInfos())
			.build();
		return new ResponseEntity<>(response, exception.getExceptionCode().getHttpStatus());
	}

	public static ResponseEntity<?> error(Set<Error> errorSet) {
		GlobalResponse response = GlobalResponse.builder()
			.success(false)
			.code(HttpStatus.BAD_REQUEST.value())
			.data(emptyList())
			.errors(errorSet)
			.meta(createMetaInfo().getMetaInfos())
			.build();
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}
}
