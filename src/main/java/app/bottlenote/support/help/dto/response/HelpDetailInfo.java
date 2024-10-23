package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.HelpImageInfo;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record HelpDetailInfo(
	Long helpId,
	String content,
	HelpType helpType,
	List<HelpImageInfo> imageUrlList,
	LocalDateTime createAt,
	StatusType statusType,

	Long adminId,
	String responseContent,
	LocalDateTime lastModifyAt
) {

}
