package app.bottlenote.alcohols.domain;

import java.util.List;

/** 주류 ID를 가진 테이블을 소유한 도메인이 구현하며, 병합 트랜잭션 안에서만 호출된다. */
public interface AlcoholReferenceMerger {

  List<AlcoholMergeCount> merge(Long sourceAlcoholId, Long targetAlcoholId);
}
