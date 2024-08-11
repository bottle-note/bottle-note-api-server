package app.bottlenote.support.help.fixture;

import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;

import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpRegisterRequest;
import app.bottlenote.support.help.dto.response.HelpRegisterResponse;

public class HelpObjectFixture {

	public static HelpRegisterRequest getHelpRegisterRequest() {
		return new HelpRegisterRequest(
			"로그인이 안돼요",
			"blah blah blah"
			, HelpType.USER
		);
	}

	public static HelpRegisterRequest getWrongTitleRegisterRequest() {
		return new HelpRegisterRequest(
			null,
			"test"
			, HelpType.USER
		);
	}

	public static HelpRegisterResponse getSuccessHelpRegisterResponse() {
		return HelpRegisterResponse.response(REGISTER_SUCCESS, 1L);
	}
}
