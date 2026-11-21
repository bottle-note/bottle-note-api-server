package app.bottlenote.user.service;

import static app.bottlenote.user.constant.UserStatus.ACTIVE;
import static app.bottlenote.user.exception.UserExceptionCode.MYBOTTLE_NOT_ACCESSIBLE;
import static app.bottlenote.user.exception.UserExceptionCode.MYPAGE_NOT_ACCESSIBLE;

import app.bottlenote.alcohols.facade.AlcoholPopularityFacade;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.request.MyBottleRequest;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.facade.UserFacade;
import app.bottlenote.user.repository.CustomUserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageQueryService {

  private final UserFacade userFacade;
  private final CustomUserRepository queryRepository;
  private final AlcoholPopularityFacade alcoholPopularityFacade;
  private final UserFilterManager userFilterManager;

  /** 해당 사용자의 기본정보를 조회 합니다 (닉네임, 프로필 이미지, 소개글) */
  @Transactional(readOnly = true)
  public MyPageResponse getMyPage(Long userId, Long currentUserId) {
    return userFilterManager.withActiveUserFilter(
        ACTIVE,
        () -> {
          boolean isUserNotAccessible = !userFacade.existsByUserId(userId);

          if (isUserNotAccessible) {
            throw new UserException(MYPAGE_NOT_ACCESSIBLE);
          }
          return queryRepository.getMyPage(userId, currentUserId);
        });
  }

  @Transactional(readOnly = true)
  public KeysetPageResponse<MyBottleResponse> getMyBottle(
      Long userId, Long currentUserId, MyBottleRequest myBottleRequest, MyBottleType myBottleType) {
    return userFilterManager.withActiveUserFilter(
        ACTIVE,
        () -> {
          if (!userFacade.existsByUserId(userId)) {
            throw new UserException(MYBOTTLE_NOT_ACCESSIBLE);
          }

          MyBottlePageableCriteria criteria =
              MyBottlePageableCriteria.of(myBottleRequest, userId, currentUserId)
                  .withHotAlcoholIds(
                      Set.copyOf(alcoholPopularityFacade.findWeeklyTopAlcoholIds(5)));

          return switch (myBottleType) {
            case REVIEW -> queryRepository.getReviewMyBottle(criteria);
            case RATING -> queryRepository.getRatingMyBottle(criteria);
            case PICK -> queryRepository.getPicksMyBottle(criteria);
          };
        });
  }
}
