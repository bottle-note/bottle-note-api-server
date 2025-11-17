package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.InMemoryUserQueryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("DefaultUserFacade 단위 테스트")
class DefaultUserFacadeTest {

  InMemoryUserQueryRepository userRepository;
  DefaultUserFacade userFacade;

  @BeforeEach
  void setUp() {
    userRepository = new InMemoryUserQueryRepository();
    userFacade = new DefaultUserFacade(userRepository);
  }

  // ========== existsByUserId ==========

  @Test
  @DisplayName("유저가 존재할 때 true를 반환할 수 있다")
  void existsByUserId_유저_존재() {
    // given
    User user = createUser("test@example.com", "테스터");
    userRepository.save(user);

    // when
    Boolean exists = userFacade.existsByUserId(user.getId());

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("유저가 존재하지 않을 때 false를 반환할 수 있다")
  void existsByUserId_유저_미존재() {
    // given
    Long nonExistentUserId = 999L;

    // when
    Boolean exists = userFacade.existsByUserId(nonExistentUserId);

    // then
    assertThat(exists).isFalse();
  }

  // ========== isValidUserId ==========

  @Test
  @DisplayName("유효한 유저 ID일 때 예외가 발생하지 않는다")
  void isValidUserId_유효한_유저() {
    // given
    User user = createUser("valid@example.com", "유효한유저");
    userRepository.save(user);

    // when & then
    assertThatCode(() -> userFacade.isValidUserId(user.getId())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("유효하지 않은 유저 ID일 때 USER_NOT_FOUND 예외가 발생한다")
  void isValidUserId_유효하지_않은_유저() {
    // given
    Long invalidUserId = 999L;

    // when & then
    assertThatThrownBy(() -> userFacade.isValidUserId(invalidUserId))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserExceptionCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("null 유저 ID일 때 예외가 발생한다")
  void isValidUserId_null_유저() {
    // when & then
    assertThatThrownBy(() -> userFacade.isValidUserId(null)).isInstanceOf(Exception.class);
  }

  // ========== getUserProfileInfo ==========

  @Test
  @DisplayName("유저 프로필 정보를 정확히 반환할 수 있다")
  void getUserProfileInfo_정상_조회() {
    // given
    User user = createUser("profile@example.com", "프로필유저");
    userRepository.save(user);

    // when
    UserProfileItem profile = userFacade.getUserProfileInfo(user.getId());

    // then
    assertThat(profile).isNotNull();
    assertThat(profile.id()).isEqualTo(user.getId());
    assertThat(profile.nickName()).isEqualTo("프로필유저");
  }

  @Test
  @DisplayName("존재하지 않는 유저 조회 시 USER_NOT_FOUND 예외가 발생한다")
  void getUserProfileInfo_존재하지_않는_유저() {
    // given
    Long nonExistentUserId = 999L;

    // when & then
    assertThatThrownBy(() -> userFacade.getUserProfileInfo(nonExistentUserId))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", UserExceptionCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("여러 유저의 프로필을 정확히 구분할 수 있다")
  void getUserProfileInfo_여러_유저_구분() {
    // given
    User user1 = createUser("user1@example.com", "유저1");
    User user2 = createUser("user2@example.com", "유저2");
    userRepository.save(user1);
    userRepository.save(user2);

    // when
    UserProfileItem profile1 = userFacade.getUserProfileInfo(user1.getId());
    UserProfileItem profile2 = userFacade.getUserProfileInfo(user2.getId());

    // then
    assertThat(profile1.nickName()).isEqualTo("유저1");
    assertThat(profile2.nickName()).isEqualTo("유저2");
    assertThat(profile1.id()).isNotEqualTo(profile2.id());
  }

  // ========== Helper Methods ==========

  private User createUser(String email, String nickName) {
    return User.builder()
        .email(email)
        .nickName(nickName)
        .age(25)
        .gender(GenderType.MALE)
        .socialType(List.of(SocialType.KAKAO))
        .role(UserType.ROLE_USER)
        .build();
  }
}
