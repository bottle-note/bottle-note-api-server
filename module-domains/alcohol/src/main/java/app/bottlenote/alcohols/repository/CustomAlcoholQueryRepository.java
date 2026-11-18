package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface CustomAlcoholQueryRepository {

  List<CategoryItem> findAllCategoryItems();

  List<AlcoholLookupItem> findAllLookupItems();

  List<AlcoholMatchTargetItem> findAllMatchTargets();

  List<AlcoholMatchTargetItem> findMatchTargetsByDistilleryIdIn(List<Long> distilleryIds);

  List<AlcoholMatchTargetItem> findMatchTargetsByIdIn(List<Long> alcoholIds);

  Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId);

  Page<AdminAlcoholItem> searchAdminAlcohols(AdminAlcoholSearchRequest request);
}
