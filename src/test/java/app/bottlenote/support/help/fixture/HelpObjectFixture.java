package app.bottlenote.support.help.fixture;

import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpRegisterRequest;

public class HelpObjectFixture {

	public static HelpRegisterRequest getHelpRegisterRequest() {
		return new HelpRegisterRequest(
			"로그인이 안돼요",
			"blah blah blah"
			, HelpType.USER
		);
	}

}
