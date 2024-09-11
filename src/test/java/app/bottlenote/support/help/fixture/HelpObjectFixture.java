package app.bottlenote.support.help.fixture;

import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpRegisterResponse;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;

public class HelpObjectFixture {

	public static HelpUpsertRequest getHelpRegisterRequest() {
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

	public static HelpRegisterResponse getSuccessHelpRegisterResponse() {
		return HelpRegisterResponse.response(REGISTER_SUCCESS, 1L);
	}
}
