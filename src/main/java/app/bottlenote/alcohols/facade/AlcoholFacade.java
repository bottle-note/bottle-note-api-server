package app.bottlenote.alcohols.facade;

import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.core.structure.Pair;
import java.util.Optional;

public interface AlcoholFacade {

  /** AlcoholInfo를 반환하는 메서드입니다. */
  Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long currentUserId);

  /** 해당 식별자의 알코올 정보와 그 다음 요소도 함께 반환하는 메서드입니다. */
  Pair<AlcoholSummaryItem, AlcoholSummaryItem> getAlcoholSummaryItemWithNext(Long alcoholId);

  /** 데이터베이스에 존재하는 Alcohol인지 검증하는 메서드입니다. */
  Boolean existsByAlcoholId(Long alcoholId);

  /** alcohol이 존재하지 않으면 예외를 던지는 메서드입니다. */
  void isValidAlcoholId(Long alcoholId);

  /** Alcohol의 이미지 URL을 반환하는 메서드입니다. */
  Optional<String> findAlcoholImageUrlById(Long alcoholId);
}
