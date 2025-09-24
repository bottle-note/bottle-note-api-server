package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.shared.alcohols.constant.AlcoholType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAlcoholQueryRepository
    extends AlcoholQueryRepository, JpaRepository<Alcohol, Long>, CustomAlcoholQueryRepository {

  @Override
  @Query(
      """
			          Select new app.bottlenote.alcohols.dto.response.CategoryItem(a.korCategory, a.engCategory,a.categoryGroup)
			          from alcohol a
			          where a.type = :type
			          group by a.korCategory, a.engCategory,a.categoryGroup
			          order by
			          	case when a.categoryGroup = app.bottlenote.shared.alcohols.constant.AlcoholCategoryGroup.OTHER then 1 else 0 end,a.korCategory
			  """)
  List<CategoryItem> findAllCategories(AlcoholType type);

  @Query("SELECT COUNT(a) > 0 FROM alcohol a WHERE a.id = :alcoholId")
  Boolean existsByAlcoholId(@Param("alcoholId") Long alcoholId);
}
