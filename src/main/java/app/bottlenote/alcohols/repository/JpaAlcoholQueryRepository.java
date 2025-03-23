package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.constant.AlcoholType;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.repository.custom.CustomAlcoholQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaAlcoholQueryRepository extends
	AlcoholQueryRepository,
	JpaRepository<Alcohol, Long>,
	CustomAlcoholQueryRepository {

	@Override
	@Query("Select new app.bottlenote.alcohols.dto.response.CategoryItem(a.korCategory, a.engCategory,a.categoryGroup) from " +
		"alcohol a where a.type = :type group by a.korCategory, a.engCategory,a.categoryGroup order by case when a.categoryGroup = 'OTHER' then 1 else 0 end, a.korCategory")
	List<CategoryItem> findAllCategories(AlcoholType type);

	@Query("SELECT COUNT(a) > 0 FROM alcohol a WHERE a.id = :alcoholId")
	Boolean existsByAlcoholId(@Param("alcoholId") Long alcoholId);
}
