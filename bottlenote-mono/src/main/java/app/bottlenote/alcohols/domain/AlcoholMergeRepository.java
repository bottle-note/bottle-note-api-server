package app.bottlenote.alcohols.domain;

import app.bottlenote.global.pagination.OffsetPage;
import java.util.List;

public interface AlcoholMergeRepository {

  /** 병합에 포함된 위스키 행을 ID 순서로 잠근다. 같은 위스키를 다루는 병합이 동시에 실행되지 않게 한다. */
  List<Alcohol> lockAlcohols(List<Long> alcoholIds);

  /** 데이터를 모두 옮긴 중복 위스키 행을 삭제한다. 인기도 관측과 스냅샷은 FK로 함께 삭제된다. */
  void deleteAlcohol(Long alcoholId);

  AlcoholMergeLog save(AlcoholMergeLog log);

  OffsetPage<AlcoholMergeLog> searchLogs(
      Long targetAlcoholId, Long sourceAlcoholId, int page, int size);
}
