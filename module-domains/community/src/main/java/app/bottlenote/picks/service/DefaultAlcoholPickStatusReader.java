package app.bottlenote.picks.service;

import app.bottlenote.alcohols.domain.AlcoholPickStatusReader;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.PicksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 주류가 정한 찜 여부 계약을 찜 저장소로 구현한다. */
@Component
@RequiredArgsConstructor
public class DefaultAlcoholPickStatusReader implements AlcoholPickStatusReader {
  private final PicksRepository picksRepository;

  @Override
  public boolean isPicked(Long alcoholId, Long userId) {
    return picksRepository.existsByAlcoholIdAndUserIdAndStatus(alcoholId, userId, PicksStatus.PICK);
  }
}
