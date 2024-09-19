package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.constant.HelpType;

import java.time.LocalDateTime;

public record HelpDetailInfo(
	Long helpId,
	String title,
	String content,
	HelpType helpType,
	LocalDateTime createAt,

	Long adminId,
	String responseContent,
	LocalDateTime lastModifyAt
) {

	public static HelpDetailInfo of (Help help) {
		return new HelpDetailInfo(help.getId(), help.getTitle(), help.getContent(), help.getType(), help.getCreateAt()
		, help.getAdminId(),help.getResponseContent() , help.getLastModifyAt());
	}
}
