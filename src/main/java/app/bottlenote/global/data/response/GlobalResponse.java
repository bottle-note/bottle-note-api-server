package app.bottlenote.global.data.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static app.bottlenote.common.service.MetaService.createMetaInfo;
import static java.util.Collections.emptyList;

/**
 * 전역 응답 객체
 */
@Getter
@Slf4j
public class GlobalResponse {
	private final Boolean success;
	private final Integer code;
	private final Object data;
	private final Object errors;
	private final Map<String, String> meta;

	public static GlobalResponse success(Object data) {
		return GlobalResponse.builder()
			.success(true)
			.code(200)
			.errors(emptyList())
			.meta(createMetaInfo())
			.data(data)
			.build();
	}

	public static GlobalResponse success(Object data, Map<String, String> meta) {
		return GlobalResponse.builder()
			.success(true)
			.code(200)
			.errors(emptyList())
			.meta(meta)
			.data(data)
			.build();
	}

	public static GlobalResponse fail(Integer code, Object errors) {
		return GlobalResponse.builder()
			.success(false)
			.code(code)
			.data(emptyList())
			.errors(errors)
			.meta(createMetaInfo())
			.build();
	}

	public static GlobalResponse fail(Integer code, Object errors, Map<String, String> meta) {
		return GlobalResponse.builder()
			.success(false)
			.code(code)
			.data(emptyList())
			.errors(errors)
			.meta(meta)
			.build();
	}

	public static GlobalResponse error(Integer code, Object errors) {
		return GlobalResponse.builder()
			.success(false)
			.code(code)
			.data(emptyList())
			.errors(errors)
			.meta(createMetaInfo())
			.build();
	}

	@Builder
	private GlobalResponse(Boolean success, Integer code, Object data, Object errors, Map<String, String> meta) {
		this.success = success;
		this.code = code;
		this.data = data;
		this.errors = errors;
		this.meta = meta;
	}
}
