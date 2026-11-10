package app.bottlenote.alcohols.domain;

import app.bottlenote.global.pagination.OffsetPage;
import java.util.List;

public interface AlcoholMergeRepository {

  /** 병합 대상 주류를 ID 순서로 잠근다. 같은 주류를 건드리는 병합끼리 순서대로 실행되게 한다. */
  List<Alcohol> lockAlcohols(List<Long> alcoholIds);

  /** 참조를 모두 옮긴 중복 주류 행을 지운다. 인기도 집계는 FK로 함께 지워진다. */
  void deleteAlcohol(Long alcoholId);

  AlcoholMergeLog save(AlcoholMergeLog log);

  OffsetPage<AlcoholMergeLog> searchLogs(
      Long targetAlcoholId, Long sourceAlcoholId, int page, int size);
}
