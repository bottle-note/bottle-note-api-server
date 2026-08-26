package app.bottlenote.alcohols.facade;

import java.util.List;

/** 타 도메인에 공개하는 인기도 조회 계약 */
public interface AlcoholPopularityFacade {

  List<Long> findWeeklyTopAlcoholIds(int limit);
}
