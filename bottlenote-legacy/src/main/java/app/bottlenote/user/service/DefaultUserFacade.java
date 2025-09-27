package app.bottlenote.user.service;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.core.users.application.UserFacade;
import app.bottlenote.shared.annotation.FacadeService;
import app.bottlenote.shared.users.payload.UserProfileItem;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FacadeService
@RequiredArgsConstructor
public class DefaultUserFacade implements UserFacade {
  private final UserRepository userQueryRepository;

  @Override
  public Boolean existsByUserId(Long userId) {
    log.info("[domain] existsByUserId : {}", userId);
    return userQueryRepository.existsByUserId(userId);
  }

  @Override
  public void isValidUserId(Long userId) {
    log.info("[domain] isValidUserId : {}", userId);

    User user =
        userQueryRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));

    log.info("[domain] isValidUserId success : {}", user.getId());
  }

  @Override
  public UserProfileItem getUserProfileInfo(Long userId) {
    User user =
        userQueryRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));

    return UserProfileItem.create(user.getId(), user.getNickName(), user.getImageUrl());
  }
}
