package app.bottlenote.global.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class GlobalResponse {
	private final String success;
	private final Integer code;
	private final List<?> data;
	private final List<?> errors;
	private final List<?> meta;

	@Builder
	public GlobalResponse(String success, Integer code, List<?> data, List<?> errors, List<?> meta) {
		this.success = success;
		this.code = code;
		this.data = data;
		this.errors = errors;
		this.meta = meta;
	}
}
