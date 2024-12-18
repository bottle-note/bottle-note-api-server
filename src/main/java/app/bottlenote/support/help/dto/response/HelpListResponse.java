package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.constant.StatusType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public record HelpListResponse(
	Long totalCount,
	List<HelpInfo> helpList

){
	public static HelpListResponse of(Long totalCount, List<HelpInfo> helpList){
		return new HelpListResponse(totalCount, helpList);
	}

	@Builder
	public record HelpInfo(
		Long helpId,
		String content,
		LocalDateTime createAt,
		StatusType helpStatus
	){

		public static HelpInfo of(Long helpId, String content, LocalDateTime createAt, StatusType statusType){
			return new HelpInfo(helpId, content, createAt, statusType);
		}
	}
}

