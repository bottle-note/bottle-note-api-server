package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.dsl.ExploreStandardCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.global.pagination.KeysetPageResponse;
import java.util.List;

/** 주류 상세·둘러보기 화면 조회 포트. 찜·별점·리뷰를 함께 읽으므로 최종 소유는 product-api다. */
public interface AlcoholDetailQueryRepository {

  AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long userId);

  KeysetPageResponse<List<AlcoholDetailItem>> getStandardExplore(ExploreStandardCriteria criteria);
}
