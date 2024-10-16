package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.domain.constant.HelpType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record HelpDetailInfo(
	Long helpId,
	String title,
	String content,
	HelpType helpType,
	LocalDateTime createAt,
	StatusType statusType,

	Long adminId,
	String responseContent,
	LocalDateTime lastModifyAt
) {

}
