package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.dto.response.Populars;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopularQueryRepository extends CrudRepository<Alcohol, Long> {

	@Query("select new app.bottlenote.alcohols.dto.response" +
            ".Populars(a.id, a.korName, a.engName,  ROUND(RAND() * 10 ) * 0.5, a.korCategory,a.engCategory, a.imageUrl) " +
            "from alcohol a " +
            "ORDER BY RAND()")
	List<Populars> getPopularOfWeek(Pageable pageable);
}
