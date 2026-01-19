package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;

public interface CustomAlcoholQueryRepository {

  PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto);

  AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long userId);

  Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId);

  Pair<Long, CursorResponse<AlcoholDetailItem>> getStandardExplore(
      Long userId, List<String> keyword, Long cursor, Integer size);

  Page<AdminAlcoholItem> searchAdminAlcohols(AdminAlcoholSearchRequest request);

  Optional<AdminAlcoholDetailProjection> findAdminAlcoholDetailById(Long alcoholId);

  record AdminAlcoholDetailProjection(
      Long alcoholId,
      String korName,
      String engName,
      String imageUrl,
      String type,
      String korCategory,
      String engCategory,
      String categoryGroup,
      String abv,
      String age,
      String cask,
      String volume,
      String description,
      Long regionId,
      String korRegion,
      String engRegion,
      Long distilleryId,
      String korDistillery,
      String engDistillery,
      Double avgRating,
      Long totalRatingsCount,
      Long reviewCount,
      Long pickCount,
      LocalDateTime createdAt,
      LocalDateTime modifiedAt) {}
}
