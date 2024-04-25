package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.dto.response.Populars;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopularQueryRepository extends CrudRepository<Alcohol, Long> {

	@Query("select new app.bottlenote.alcohols.dto.response.Populars(a.id, a.korName, a.engName, sum(r.rating), a.category, a.imageUrl) " +
		"from alcohol a " +
		"left outer join rating r on a.id = r.alcohol.id " +
		"where r.createAt >= current_date - 7 " +
		"group by a.id, a.korName, a.engName, a.category, a.imageUrl " +
		"having sum(r.rating) > 0 " +
		"order by sum(r.rating) desc " +
		"limit :top")
	List<Populars> getPopularOfWeek(Integer top);

}
