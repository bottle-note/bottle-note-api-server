package app.bottlenote.support.business.dto.response;

import app.bottlenote.support.constant.StatusType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BusinessInfoResponse(
		Long id,
		String content,
		LocalDateTime createAt,
		StatusType status
) {
		public static BusinessInfoResponse of(Long id, String content, LocalDateTime createAt, StatusType status) {
			return new BusinessInfoResponse(id, content, createAt, status);
		}
	}
