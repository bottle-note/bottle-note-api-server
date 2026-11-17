package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static java.lang.Boolean.FALSE;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholPickStatusReader;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.common.annotation.FacadeService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@FacadeService
@RequiredArgsConstructor
public class DefaultAlcoholFacade implements AlcoholFacade {
  private final AlcoholQueryRepository alcoholQueryRepository;
  private final AlcoholPickStatusReader alcoholPickStatusReader;

  @Override
  public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId) {
    return alcoholQueryRepository
        .findAlcoholInfoById(alcoholId, userId)
        .map(item -> withPicked(item, userId));
  }

  // 비로그인(null, -1)은 찜 조회 없이 false로 둔다.
  private AlcoholSummaryItem withPicked(AlcoholSummaryItem item, Long userId) {
    boolean picked =
        userId != null
            && userId != -1L
            && alcoholPickStatusReader.isPicked(item.alcoholId(), userId);
    return new AlcoholSummaryItem(
        item.alcoholId(),
        item.korName(),
        item.engName(),
        item.korCategoryName(),
        item.engCategoryName(),
        item.imageUrl(),
        picked);
  }

  @Override
  public Pair<AlcoholSummaryItem, AlcoholSummaryItem> getAlcoholSummaryItemWithNext(
      Long alcoholId) {

    AlcoholSummaryItem firstItem =
        alcoholQueryRepository.findAlcoholInfoById(alcoholId, null).orElse(null);

    if (firstItem == null) {
      return Pair.of(null, null);
    }

    AlcoholSummaryItem secondItem =
        alcoholQueryRepository.findAlcoholInfoById(alcoholId - 1, null).orElse(null);

    return Pair.of(firstItem, secondItem);
  }

  @Override
  public Boolean existsByAlcoholId(Long alcoholId) {
    return alcoholQueryRepository.existsByAlcoholId(alcoholId);
  }

  @Override
  public void isValidAlcoholId(Long alcoholId) {
    if (existsByAlcoholId(alcoholId).equals(FALSE)) {
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    }
  }

  @Override
  public Optional<String> findAlcoholImageUrlById(Long alcoholId) {
    isValidAlcoholId(alcoholId);
    return alcoholQueryRepository.findById(alcoholId).map(Alcohol::getImageUrl);
  }
}
