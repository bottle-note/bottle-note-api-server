package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.AlcoholViewCounter;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholPopularitySnapshotRepository;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.history.service.AlcoholViewHistoryService;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.facade.ReviewFacade;
import app.bottlenote.user.facade.FollowFacade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("AlcoholQueryService 단위 테스트")
class AlcoholQueryServiceTest {

  @Mock private AlcoholQueryRepository alcoholQueryRepository;
  @Mock private AlcoholViewCounter alcoholViewCounter;
  @Mock private AlcoholViewHistoryService viewHistoryService;
  @Mock private ReviewFacade reviewFacade;
  @Mock private FollowFacade followFacade;
  @Mock private AlcoholReferenceService alcoholReferenceService;
  @Mock private HmacCursorCodec cursorCodec;

  private AlcoholQueryService alcoholQueryService;

  @BeforeEach
  void setUp() {
    alcoholQueryService =
        new AlcoholQueryService(
            alcoholQueryRepository,
            new InMemoryAlcoholPopularitySnapshotRepository(),
            alcoholViewCounter,
            viewHistoryService,
            reviewFacade,
            followFacade,
            alcoholReferenceService,
            cursorCodec);
  }

  @Test
  @DisplayName("회원이 상세 조회에 성공하면 공용 카운터와 개인 조회 기록을 저장한다")
  void findAlcoholDetailById_whenMemberSucceeds_recordsCounterAndPersonalHistory() {
    // given
    long alcoholId = 42L;
    long userId = 7L;
    AlcoholDetailItem detail = AlcoholDetailItem.builder().alcoholId(alcoholId).build();
    when(alcoholQueryRepository.findAlcoholDetailById(alcoholId, userId)).thenReturn(detail);
    when(followFacade.getTastingFriendsInfoList(any(), any(), any(PageRequest.class)))
        .thenReturn(List.of());
    when(reviewFacade.getReviewInfoList(alcoholId, userId))
        .thenReturn(ReviewListResponse.of(List.of()));

    // when
    alcoholQueryService.findAlcoholDetailById(alcoholId, userId);

    // then
    verify(viewHistoryService).recordView(userId, detail);
    verify(alcoholViewCounter).increment(alcoholId);
  }

  @Test
  @DisplayName("비회원이 상세 조회에 성공하면 공용 카운터만 증가시킨다")
  void findAlcoholDetailById_whenGuestSucceeds_recordsOnlyCounter() {
    // given
    long alcoholId = 42L;
    long guestId = -1L;
    AlcoholDetailItem detail = AlcoholDetailItem.builder().alcoholId(alcoholId).build();
    when(alcoholQueryRepository.findAlcoholDetailById(alcoholId, guestId)).thenReturn(detail);
    when(followFacade.getTastingFriendsInfoList(any(), any(), any(PageRequest.class)))
        .thenReturn(List.of());
    when(reviewFacade.getReviewInfoList(alcoholId, guestId))
        .thenReturn(ReviewListResponse.of(List.of()));

    // when
    alcoholQueryService.findAlcoholDetailById(alcoholId, guestId);

    // then
    verifyNoInteractions(viewHistoryService);
    verify(alcoholViewCounter).increment(alcoholId);
  }

  @Test
  @DisplayName("상세 조회가 실패하면 공용 카운터를 증가시키지 않는다")
  void findAlcoholDetailById_whenAlcoholDoesNotExist_doesNotRecordCounter() {
    // given
    long alcoholId = 42L;
    long guestId = -1L;
    when(alcoholQueryRepository.findAlcoholDetailById(alcoholId, guestId)).thenReturn(null);

    // when & then
    assertThatThrownBy(() -> alcoholQueryService.findAlcoholDetailById(alcoholId, guestId))
        .isInstanceOf(AlcoholException.class);
    verify(alcoholViewCounter, never()).increment(any());
    verifyNoInteractions(viewHistoryService);
  }

  @Test
  @DisplayName("SSR 호출이면 상세 응답은 반환하되 공용 카운터를 증가시키지 않는다")
  void SSR_호출이면_상세_응답을_반환하고_카운터를_증가시키지_않을_수_있다() {
    // given
    long alcoholId = 42L;
    long userId = 7L;
    AlcoholDetailItem detail = AlcoholDetailItem.builder().alcoholId(alcoholId).build();
    when(alcoholQueryRepository.findAlcoholDetailById(alcoholId, userId)).thenReturn(detail);
    when(followFacade.getTastingFriendsInfoList(any(), any(), any(PageRequest.class)))
        .thenReturn(List.of());
    when(reviewFacade.getReviewInfoList(alcoholId, userId))
        .thenReturn(ReviewListResponse.of(List.of()));

    // when
    AlcoholDetailResponse response =
        alcoholQueryService.findAlcoholDetailById(alcoholId, userId, true);

    // then
    assertThat(response.alcohols().getAlcoholId()).isEqualTo(alcoholId);
    verify(viewHistoryService).recordView(userId, detail);
    verify(alcoholViewCounter, never()).increment(any());
  }

  @Test
  @DisplayName("SSR이 아니면 회원과 비회원 모두 공용 카운터를 증가시킨다")
  void SSR이_아니면_회원과_비회원_모두_카운터를_증가시킬_수_있다() {
    // given
    long alcoholId = 42L;
    long userId = 7L;
    long guestId = -1L;
    AlcoholDetailItem detail = AlcoholDetailItem.builder().alcoholId(alcoholId).build();
    when(alcoholQueryRepository.findAlcoholDetailById(alcoholId, userId)).thenReturn(detail);
    when(alcoholQueryRepository.findAlcoholDetailById(alcoholId, guestId)).thenReturn(detail);
    when(followFacade.getTastingFriendsInfoList(any(), any(), any(PageRequest.class)))
        .thenReturn(List.of());
    when(reviewFacade.getReviewInfoList(any(), any())).thenReturn(ReviewListResponse.of(List.of()));

    // when
    AlcoholDetailResponse memberResponse =
        alcoholQueryService.findAlcoholDetailById(alcoholId, userId, false);
    AlcoholDetailResponse guestResponse =
        alcoholQueryService.findAlcoholDetailById(alcoholId, guestId, false);

    // then
    assertThat(memberResponse.alcohols().getAlcoholId()).isEqualTo(alcoholId);
    assertThat(guestResponse.alcohols().getAlcoholId()).isEqualTo(alcoholId);
    verify(viewHistoryService).recordView(userId, detail);
    verify(alcoholViewCounter, times(2)).increment(alcoholId);
  }

  @Test
  @DisplayName("SSR 호출이라도 대상이 없으면 예외가 나고 공용 카운터는 증가하지 않는다")
  void SSR_호출이라도_대상이_없으면_예외가_나고_카운터는_증가하지_않을_수_있다() {
    // given
    long alcoholId = 42L;
    long guestId = -1L;
    when(alcoholQueryRepository.findAlcoholDetailById(alcoholId, guestId)).thenReturn(null);

    // when & then
    assertThatThrownBy(() -> alcoholQueryService.findAlcoholDetailById(alcoholId, guestId, true))
        .isInstanceOf(AlcoholException.class);
    verify(alcoholViewCounter, never()).increment(any());
    verifyNoInteractions(viewHistoryService);
  }
}
