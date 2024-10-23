package app.bottlenote.support.help.fixture;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.HelpImageInfo;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpDetailInfo;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.dto.response.constant.HelpResultMessage;
import java.time.LocalDateTime;
import java.util.List;

public class HelpObjectFixture {

	public static Help getHelpDefaultFixture(){
		return Help.create(1L,HelpType.USER, "blah blah blah");
	}

	public static Help getHelpFixure(String content, HelpType helpType){
		return Help.create(1L, helpType, content);
	}

	public static HelpUpsertRequest getHelpUpsertRequest() {
		return new HelpUpsertRequest(
			"로그인이 안돼요",
			HelpType.USER,
			List.of(new HelpImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"))
		);
	}

	public static HelpUpsertRequest getWrongTitleRegisterRequest() {
		return new HelpUpsertRequest(
			null,
			HelpType.USER,
			List.of(new HelpImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"))
		);
	}

	public static HelpResultResponse getSuccessHelpResponse(HelpResultMessage helpResultMessage) {
		return HelpResultResponse.response(helpResultMessage, 1L);
	}

	public static HelpPageableRequest getEmptyHelpPageableRequest(){
		return HelpPageableRequest.builder().build();
	}

	public static PageResponse<HelpListResponse> getHelpListPageResponse(){

		List<HelpListResponse.HelpInfo> helpInfos = List.of(
			HelpListResponse.HelpInfo.of(1L, "test1", LocalDateTime.now(), StatusType.WAITING),
			HelpListResponse.HelpInfo.of(2L, "test2", LocalDateTime.now(), StatusType.WAITING)
		);
		return PageResponse.of(
			HelpListResponse.of((long)helpInfos.size(), helpInfos),
			CursorPageable.builder()
				.currentCursor(0L)
				.cursor(1L)
				.pageSize((long) helpInfos.size())
				.hasNext(false)
				.build());
	}

	public static HelpListResponse getHelpListResponse(List<HelpListResponse.HelpInfo> helpInfoList){
		return HelpListResponse.of((long) helpInfoList.size(), helpInfoList);
	}

	public static HelpDetailInfo getDetailHelpInfo(String content, HelpType type) {
		return HelpDetailInfo.builder()
			.helpId(1L)
			.responseContent(null)
			.imageUrlList(
				List.of(
					HelpImageInfo.create(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")
				)
			)
			.lastModifyAt(LocalDateTime.now())
			.createAt(LocalDateTime.now())
			.content(content)
			.helpType(type)
			.build();
	}
}
