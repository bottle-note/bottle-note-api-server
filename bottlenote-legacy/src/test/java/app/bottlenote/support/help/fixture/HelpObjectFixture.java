package app.bottlenote.support.help.fixture;

import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.constant.HelpResultMessage;
import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.dto.request.HelpImageItem;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpDetailItem;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import java.time.LocalDateTime;
import java.util.List;

public class HelpObjectFixture {

  public static Help getHelpDefaultFixture() {
    return Help.create(1L, HelpType.USER, "기본 제목", "blah blah blah");
  }

  public static Help getHelpFixure(String content, HelpType helpType) {
    return Help.create(1L, helpType, "테스트 제목", content);
  }

  public static HelpUpsertRequest getHelpUpsertRequest() {
    return new HelpUpsertRequest(
        "문의 제목",
        "로그인이 안돼요",
        HelpType.USER,
        List.of(
            new HelpImageItem(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")));
  }

  public static HelpUpsertRequest getWrongTitleRegisterRequest() {
    return new HelpUpsertRequest(
        null,
        "로그인이 안돼요",
        HelpType.USER,
        List.of(
            new HelpImageItem(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")));
  }

  public static HelpResultResponse getSuccessHelpResponse(HelpResultMessage helpResultMessage) {
    return HelpResultResponse.response(helpResultMessage, 1L);
  }

  public static HelpPageableRequest getEmptyHelpPageableRequest() {
    return HelpPageableRequest.builder().build();
  }

  public static PageResponse<HelpListResponse> getHelpListPageResponse() {

    List<HelpListResponse.HelpInfo> helpInfos =
        List.of(
            HelpListResponse.HelpInfo.of(
                1L, "제목1", "test1", LocalDateTime.now(), StatusType.WAITING),
            HelpListResponse.HelpInfo.of(
                2L, "제목2", "test2", LocalDateTime.now(), StatusType.WAITING));
    return PageResponse.of(
        HelpListResponse.of((long) helpInfos.size(), helpInfos),
        CursorPageable.builder()
            .currentCursor(0L)
            .cursor(1L)
            .pageSize((long) helpInfos.size())
            .hasNext(false)
            .build());
  }

  public static HelpListResponse getHelpListResponse(List<HelpListResponse.HelpInfo> helpInfoList) {
    return HelpListResponse.of((long) helpInfoList.size(), helpInfoList);
  }

  public static HelpDetailItem getDetailHelpInfo(String content, HelpType type) {
    return HelpDetailItem.builder()
        .helpId(1L)
        .title("상세 제목")
        .responseContent(null)
        .imageUrlList(
            List.of(
                HelpImageItem.create(
                    1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")))
        .lastModifyAt(LocalDateTime.now())
        .createAt(LocalDateTime.now())
        .content(content)
        .helpType(type)
        .build();
  }
}
