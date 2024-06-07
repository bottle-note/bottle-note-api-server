package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.constant.AlcoholType;
import app.bottlenote.alcohols.dto.response.CategoryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaAlcoholQueryRepository extends
	AlcoholQueryRepository,
	JpaRepository<Alcohol, Long>,
	CustomAlcoholQueryRepository {

	@Query("Select new app.bottlenote.alcohols.dto.response.CategoryResponse(a.korCategory, a.engCategory) from " +
            "alcohol a where a.type = :type group by a.korCategory, a.engCategory")
	@Override
	List<CategoryResponse> findAllCategories(AlcoholType type);
}
