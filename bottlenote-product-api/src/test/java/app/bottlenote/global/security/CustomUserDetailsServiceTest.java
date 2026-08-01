package app.bottlenote.global.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.repository.OauthRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Tag("unit")
@DisplayName("[unit] CustomUserDetailsService")
class CustomUserDetailsServiceTest {

  @Test
  @DisplayName("가입 대기 사용자는 일반 인증 주체가 될 수 없다")
  void signup_pending_user_cannot_authenticate() {
    // given
    OauthRepository oauthRepository = mock(OauthRepository.class);
    User pendingUser =
        User.builder()
            .id(1L)
            .email("pending@test.com")
            .nickName("가입대기유저")
            .role(UserType.ROLE_USER)
            .status(UserStatus.SIGNUP_PENDING)
            .socialType(List.of(SocialType.KAKAO))
            .build();
    when(oauthRepository.findByEmail(pendingUser.getEmail())).thenReturn(Optional.of(pendingUser));
    CustomUserDetailsService service = new CustomUserDetailsService(oauthRepository);

    // when & then
    assertThatThrownBy(() -> service.loadUserByUsername(pendingUser.getEmail()))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
