package app.bottlenote.user.service;

import static app.bottlenote.user.constant.UserStatus.ACTIVE;
import static app.bottlenote.user.constant.WithdrawUserResultMessage.USER_WITHDRAW_SUCCESS;
import static app.bottlenote.user.exception.UserExceptionCode.MYBOTTLE_NOT_ACCESSIBLE;
import static app.bottlenote.user.exception.UserExceptionCode.MYPAGE_NOT_ACCESSIBLE;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import app.bottlenote.common.file.event.payload.ImageResourceInvalidatedEvent;
import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.request.MyBottleRequest;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.dto.response.WithdrawUserResultResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserBasicService {

  private static final String REFERENCE_TYPE_PROFILE = "PROFILE";

  private final UserRepository userRepository;
  private final UserFilterManager userFilterManager;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public NicknameChangeResponse nicknameChange(Long userId, NicknameChangeRequest request) {
    return userFilterManager.withActiveUserFilter(
        ACTIVE,
        () -> {
          String name = request.nickName();
          String beforeNickname;

          if (userRepository.existsByNickName(name)) {
            throw new UserException(UserExceptionCode.USER_NICKNAME_NOT_VALID);
          }

          User user =
              userRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));

          beforeNickname = user.getNickName();

          user.changeNickName(name);
          Long updatedUser = user.getId();
          String newUserNickName = user.getNickName();

          return NicknameChangeResponse.builder()
              .message(NicknameChangeResponse.Message.SUCCESS)
              .userId(updatedUser)
              .beforeNickname(beforeNickname)
              .changedNickname(newUserNickName)
              .build();
        });
  }

  @Transactional
  public ProfileImageChangeResponse profileImageChange(Long userId, String viewUrl) {
    return userFilterManager.withActiveUserFilter(
        ACTIVE,
        () -> {
          User user =
              userRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));

          // 기존 프로필 이미지 URL 저장 (교체 전)
          String oldImageUrl = user.getImageUrl();

          user.changeProfileImage(viewUrl);

          // 기존 이미지가 있고 새 이미지와 다른 경우 INVALIDATED 이벤트 발행
          if (oldImageUrl != null && !oldImageUrl.equals(viewUrl)) {
            String oldResourceKey = ImageUtil.extractResourceKey(oldImageUrl);
            if (oldResourceKey != null && user.getId() != null) {
              eventPublisher.publishEvent(
                  ImageResourceInvalidatedEvent.of(
                      oldResourceKey, user.getId(), REFERENCE_TYPE_PROFILE));
            }
          }

          // 새 이미지에 대해 ACTIVATED 이벤트 발행
          String resourceKey = ImageUtil.extractResourceKey(viewUrl);
          if (resourceKey != null && user.getId() != null) {
            eventPublisher.publishEvent(
                ImageResourceActivatedEvent.of(resourceKey, user.getId(), REFERENCE_TYPE_PROFILE));
          }

          return new ProfileImageChangeResponse(user.getId(), user.getImageUrl());
        });
  }

  @Transactional
  public WithdrawUserResultResponse withdrawUser(Long userId) {
    return userFilterManager.withActiveUserFilter(
        ACTIVE,
        () -> {
          User user =
              userRepository.findById(userId).orElseThrow(() -> new UserException(USER_NOT_FOUND));

          user.withdrawUser();

          return WithdrawUserResultResponse.response(USER_WITHDRAW_SUCCESS, userId);
        });
  }

  /** 해당 사용자의 기본정보를 조회 합니다 (닉네임, 프로필 이미지, 소개글) */
  @Transactional(readOnly = true)
  public MyPageResponse getMyPage(Long userId, Long currentUserId) {
    return userFilterManager.withActiveUserFilter(
        ACTIVE,
        () -> {
          boolean isUserNotAccessible = !userRepository.existsByUserId(userId);

          if (isUserNotAccessible) {
            throw new UserException(MYPAGE_NOT_ACCESSIBLE);
          }
          return userRepository.getMyPage(userId, currentUserId);
        });
  }

  @Transactional(readOnly = true)
  public PageResponse<MyBottleResponse> getMyBottle(
      Long userId, Long currentUserId, MyBottleRequest myBottleRequest, MyBottleType myBottleType) {
    return userFilterManager.withActiveUserFilter(
        ACTIVE,
        () -> {
          if (!userRepository.existsByUserId(userId)) {
            throw new UserException(MYBOTTLE_NOT_ACCESSIBLE);
          }

          MyBottlePageableCriteria criteria =
              MyBottlePageableCriteria.of(myBottleRequest, userId, currentUserId);

          return switch (myBottleType) {
            case REVIEW -> userRepository.getReviewMyBottle(criteria);
            case RATING -> userRepository.getRatingMyBottle(criteria);
            case PICK -> userRepository.getPicksMyBottle(criteria);
          };
        });
  }
}
