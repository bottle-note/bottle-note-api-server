package app.bottlenote.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("User 엔티티 단위 테스트")
class UserTest {

  @Test
  @DisplayName("lastLoginAt이 null인 사용자는 최초 로그인 상태이다")
  void isFirstLogin_when_lastLoginAt_is_null() {
    // given
    User user =
        User.builder()
            .id(1L)
            .email("test@example.com")
            .nickName("부드러운몰트1234")
            .role(UserType.ROLE_USER)
            .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
            .lastLoginAt(null)
            .build();

    // when
    boolean isFirstLogin = user.isFirstLogin();

    // then
    assertThat(isFirstLogin).isTrue();
  }

  @Test
  @DisplayName("lastLoginAt이 있는 사용자는 최초 로그인 상태가 아니다")
  void isNotFirstLogin_when_lastLoginAt_exists() {
    // given
    User user =
        User.builder()
            .id(1L)
            .email("test@example.com")
            .nickName("나만의닉네임")
            .role(UserType.ROLE_USER)
            .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
            .lastLoginAt(LocalDateTime.now())
            .build();

    // when
    boolean isFirstLogin = user.isFirstLogin();

    // then
    assertThat(isFirstLogin).isFalse();
  }

  @Test
  @DisplayName("로그인 시간 업데이트 후 최초 로그인 상태가 아니다")
  void isNotFirstLogin_after_updateLastLoginAt() {
    // given
    User user =
        User.builder()
            .id(1L)
            .email("test@example.com")
            .nickName("부드러운몰트1234")
            .role(UserType.ROLE_USER)
            .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
            .build();

    assertThat(user.isFirstLogin()).isTrue();

    // when
    user.updateLastLoginAt(LocalDateTime.now());

    // then
    assertThat(user.isFirstLogin()).isFalse();
  }

  @Test
  @DisplayName("기본값으로 생성된 사용자는 lastLoginAt이 null이다")
  void default_lastLoginAt_is_null() {
    // given
    User user =
        User.builder()
            .id(1L)
            .email("test@example.com")
            .nickName("부드러운몰트1234")
            .role(UserType.ROLE_USER)
            .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
            .build();

    // when & then
    assertThat(user.isFirstLogin()).isTrue();
  }

  @Test
  @DisplayName("닉네임 변경 시 닉네임이 정상적으로 업데이트된다")
  void changeNickName_updates_nickname() {
    // given
    User user =
        User.builder()
            .id(1L)
            .email("test@example.com")
            .nickName("부드러운몰트1234")
            .role(UserType.ROLE_USER)
            .socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
            .build();

    // when
    user.changeNickName("새로운닉네임");

    // then
    assertThat(user.getNickName()).isEqualTo("새로운닉네임");
  }
}
