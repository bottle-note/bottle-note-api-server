package app.bottlenote.user.integration;

import app.bottlenote.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

@Tag("integration")
@DisplayName("[integration] [controller] UserQueryController")
public class UserQueryIntegration extends IntegrationTestSupport {

	@DisplayName("마이페이지 유저 정보를 조회할 수 있다.")
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-review.sql",
		"/init-script/init-rating.sql",
		"/init-script/init-picks.sql",
		"/init-script/init-follow.sql",
	}
	)

	@Test
	void test_1() throws Exception {
		// given
		// when && then
	}

}
