package app.bottlenote.user.fixture;

import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import java.util.ArrayList;
import java.util.List;

public class UserObjectFixture {

  public static User getUserFixtureObject() {
    return User.builder()
        .id(1L)
        .email("test@email.com")
        .age(25)
        .gender(GenderType.MALE)
        .role(UserType.ROLE_USER)
        .imageUrl("profileurl")
        .nickName("nickname")
        .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
        .build();
  }

  public static User getUserFixtureObject(String email) {
    return User.builder()
        .id(1L)
        .email(email)
        .age(25)
        .gender(GenderType.MALE)
        .role(UserType.ROLE_USER)
        .imageUrl("profileurl")
        .nickName("nickname")
        .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
        .build();
  }

  public static User getUserFixtureObject(String email, int age) {
    return User.builder()
        .id(1L)
        .email(email)
        .age(age)
        .gender(GenderType.MALE)
        .role(UserType.ROLE_USER)
        .imageUrl("profileurl")
        .nickName("nickname")
        .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
        .build();
  }
}
