package app.bottlenote.support.business.dto.response;

import app.bottlenote.support.constant.StatusType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BusinessSupportDetailItem(
		Long id,
		String content,
		String contactWay,
		LocalDateTime createAt,
		StatusType status,
		Long adminId,
		String responseContent,
		LocalDateTime lastModifyAt
) {
}
