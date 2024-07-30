package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserStatus;
import app.bottlenote.user.domain.constant.UserType;

public class UserObjectFixture {

	public static User getUserFixtureObject() {
		return User.builder()
			.id(1L)
			.email("test@email.com")
			.age(25)
			.gender(String.valueOf(GenderType.MALE))
			.role(UserType.ROLE_USER)
			.imageUrl("profileurl")
			.nickName("nickname")
			.status(UserStatus.ACTIVE)
			.socialType(SocialType.KAKAO)
			.build();
	}

	public static User getUserFixtureObject(String email) {
		return User.builder()
			.id(1L)
			.email(email)
			.age(25)
			.gender(String.valueOf(GenderType.MALE))
			.role(UserType.ROLE_USER)
			.imageUrl("profileurl")
			.nickName("nickname")
			.status(UserStatus.ACTIVE)
			.socialType(SocialType.KAKAO)
			.build();
	}

	public static User getUserFixtureObject(String email, int age) {
		return User.builder()
			.id(1L)
			.email(email)
			.age(age)
			.gender(String.valueOf(GenderType.MALE))
			.role(UserType.ROLE_USER)
			.imageUrl("profileurl")
			.nickName("nickname")
			.status(UserStatus.ACTIVE)
			.socialType(SocialType.KAKAO)
			.build();
	}

}
