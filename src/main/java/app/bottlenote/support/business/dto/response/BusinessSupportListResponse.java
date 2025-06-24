package app.bottlenote.support.business.dto.response;

import app.bottlenote.support.constant.StatusType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public record BusinessSupportListResponse(Long totalCount, List<BusinessInfo> list) {
	public static BusinessSupportListResponse of(Long totalCount, List<BusinessInfo> list) {
		return new BusinessSupportListResponse(totalCount, list);
	}

	@Builder
	public record BusinessInfo(Long id, String content, LocalDateTime createAt, StatusType status) {
		public static BusinessInfo of(Long id, String content, LocalDateTime createAt, StatusType status) {
			return new BusinessInfo(id, content, createAt, status);
		}
	}
}
