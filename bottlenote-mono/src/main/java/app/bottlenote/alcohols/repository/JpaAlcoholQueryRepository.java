package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAlcoholQueryRepository
    extends AlcoholQueryRepository, JpaRepository<Alcohol, Long>, CustomAlcoholQueryRepository {

  @Override
  @Query("select distinct a from alcohol a left join fetch a.region where a.id in :ids")
  List<Alcohol> findAllByIdIn(@Param("ids") List<Long> ids);

  @Override
  @Query(
      """
			        Select new app.bottlenote.alcohols.dto.response.CategoryItem(a.korCategory, a.engCategory,a.categoryGroup)
			        from alcohol a
			        where a.type = :type
			        and a.deletedAt is null
			        group by a.korCategory, a.engCategory,a.categoryGroup
			        order by
			        	case when a.categoryGroup = app.bottlenote.alcohols.constant.AlcoholCategoryGroup.OTHER then 1 else 0 end,a.korCategory
			""")
  List<CategoryItem> findAllCategories(AlcoholType type);

  @Query("SELECT COUNT(a) > 0 FROM alcohol a WHERE a.id = :alcoholId AND a.deletedAt IS NULL")
  Boolean existsByAlcoholId(@Param("alcoholId") Long alcoholId);

  @Query("SELECT COUNT(a) > 0 FROM alcohol a WHERE a.distillery.id = :distilleryId")
  Boolean existsByDistilleryId(@Param("distilleryId") Long distilleryId);
}
