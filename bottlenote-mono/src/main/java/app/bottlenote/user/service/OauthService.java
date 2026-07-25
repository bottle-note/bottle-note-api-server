package app.bottlenote.user.service;

import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.OauthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {
  private final OauthRepository oauthRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Transactional
  public void restoreUser(String email, String password) {
    User user =
        oauthRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    if (user.isAlive()) throw new UserException(UserExceptionCode.USER_ALREADY_EXISTS);

    if (user.getSocialType().contains(SocialType.BASIC)) {
      boolean matches = passwordEncoder.matches(password, user.getPassword());
      if (!matches) {
        throw new UserException(UserExceptionCode.INVALID_PASSWORD);
      }
    }

    user.restore();
  }
}
