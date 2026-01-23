package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.response.PopularItem;
import java.util.List;

/** 조회수 기반 인기 주류 조회를 위한 QueryDSL Custom Repository */
public interface CustomPopularQueryRepository {

  List<PopularItem> getPopularByViewsWeekly(Long userId, int limit);

  List<PopularItem> getPopularByViewsMonthly(Long userId, int limit);
}
