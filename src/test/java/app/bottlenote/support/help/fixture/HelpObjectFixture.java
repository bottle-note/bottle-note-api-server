package app.bottlenote.support.help.fixture;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.dto.response.constant.HelpResultMessage;

import java.time.LocalDateTime;
import java.util.List;

public class HelpObjectFixture {

	public static Help getHelpDefaultFixture(){
		return Help.create(1L, "로그인이 안돼요", "blah blah blah", HelpType.USER);
	}

	public static Help getHelpFixure(String title, String content, HelpType helpType){
		return Help.create(1L, title, content, helpType);
	}

	public static HelpUpsertRequest getHelpUpsertRequest() {
		return new HelpUpsertRequest(
			"로그인이 안돼요",
			"blah blah blah"
			, HelpType.USER
		);
	}

	public static HelpUpsertRequest getWrongTitleRegisterRequest() {
		return new HelpUpsertRequest(
			null,
			"test"
			, HelpType.USER
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
			HelpListResponse.HelpInfo.of(1L, "test1", LocalDateTime.now()),
			HelpListResponse.HelpInfo.of(2L, "test2", LocalDateTime.now())
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

	public HelpListResponse getHelpListResponse(List<HelpListResponse.HelpInfo> helpInfoList){
		return HelpListResponse.of((long) helpInfoList.size(), helpInfoList);
	}
}
