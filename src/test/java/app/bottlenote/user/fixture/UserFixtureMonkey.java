package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.BuilderArbitraryIntrospector;
import java.util.List;

public class UserFixtureMonkey {

	private static final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
		.objectIntrospector(BuilderArbitraryIntrospector.INSTANCE)
		.build();

	public static User createUserFixture() {
		return fixtureMonkey.giveMeBuilder(User.class)
			.set("id", 1L)
			.set("email", "test@email.com")
			.set("age", 25)
			.set("gender", GenderType.MALE)
			.set("role", UserType.ROLE_USER)
			.set("imageUrl", "profileurl")
			.set("nickName", "nickname")
			.set("socialType", List.of(SocialType.KAKAO))
			.sample();
	}
}