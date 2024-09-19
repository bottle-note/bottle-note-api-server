package app.bottlenote.support.help.repository.custom;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.response.HelpListResponse;

public interface CustomHelpQueryRepository {

	/**
	 * 문의글 목록을 조회하는 메서드입니다.
	 *
	 * @param helpPageableRequest
	 * @param currentUserId
	 * @return
	 */
	PageResponse<HelpListResponse> getHelpList (HelpPageableRequest helpPageableRequest, Long currentUserId);
}
