package app.bottlenote.common.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RestdocsResponse {
	private final String message;
	private final LocalDateTime responseAt;

	public RestdocsResponse(LocalDateTime restdocs) {
		this.message = "RestDocs is running";
		this.responseAt = restdocs;
	}
}
