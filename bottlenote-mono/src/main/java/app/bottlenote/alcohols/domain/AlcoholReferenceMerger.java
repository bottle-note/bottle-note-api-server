package app.bottlenote.alcohols.domain;

import java.util.List;

/** 위스키 ID를 저장하는 테이블을 가진 도메인이 구현한다. 병합 트랜잭션 안에서만 호출된다. */
public interface AlcoholReferenceMerger {

  List<AlcoholMergeCount> merge(Long sourceAlcoholId, Long targetAlcoholId);
}
