package app.bottlenote.support.help.repository.custom;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.dto.request.AdminHelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.response.AdminHelpListResponse;
import app.bottlenote.support.help.dto.response.HelpListResponse;

public interface CustomHelpQueryRepository {

  /**
   * 문의글 목록을 조회하는 메서드입니다. (사용자용)
   *
   * @param helpPageableRequest 페이징 요청
   * @param currentUserId 현재 사용자 ID
   * @return 문의글 목록
   */
  PageResponse<HelpListResponse> getHelpList(
      HelpPageableRequest helpPageableRequest, Long currentUserId);

  /**
   * 문의글 목록을 조회하는 메서드입니다. (관리자용)
   *
   * @param request 페이징 및 필터링 요청
   * @return 전체 문의글 목록
   */
  PageResponse<AdminHelpListResponse> getAdminHelpList(AdminHelpPageableRequest request);
}
