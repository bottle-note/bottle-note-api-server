package app.bottlenote.global.response;

import lombok.Builder;
import lombok.Getter;

import static java.util.Collections.emptyList;

/**
 * 전역 응답 객체
 */
@Getter
public class GlobalResponse {
	private final String success;
	private final Integer code;
	private final Object data;
	private final Object errors;
	private final Object meta;

	public static GlobalResponse success(Object data) {
		return GlobalResponse.builder()
			.success("true")
			.code(200)
			.errors(emptyList())
			.meta(emptyList())
			.data(data)
			.build();
	}

	public static GlobalResponse success(Object data, Object meta) {
		return GlobalResponse.builder()
			.success("true")
			.code(200)
			.errors(emptyList())
			.meta(meta)
			.data(data)
			.build();
	}

	public static GlobalResponse fail(Integer code, Object errors) {
		return GlobalResponse.builder()
			.success("fail")
			.code(code)
			.data(emptyList())
			.errors(errors)
			.meta(emptyList())
			.build();
	}

	public static GlobalResponse fail(Integer code, Object errors, Object meta) {
		return GlobalResponse.builder()
			.success("fail")
			.code(code)
			.data(emptyList())
			.errors(errors)
			.meta(meta)
			.build();
	}

	public static GlobalResponse error(Integer code, Object errors) {
		return GlobalResponse.builder()
			.success("error")
			.code(code)
			.data(emptyList())
			.errors(errors)
			.meta(emptyList())
			.build();
	}

	@Builder
	private GlobalResponse(String success, Integer code, Object data, Object errors, Object meta) {
		this.success = success;
		this.code = code;
		this.data = data;
		this.errors = errors;
		this.meta = meta;
	}
}
