package app.bottlenote.support.help.fixture;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpUpsertResponse;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;

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

	public static HelpUpsertResponse getSuccessHelpRegisterResponse() {
		return HelpUpsertResponse.response(REGISTER_SUCCESS, 1L);
	}
}
