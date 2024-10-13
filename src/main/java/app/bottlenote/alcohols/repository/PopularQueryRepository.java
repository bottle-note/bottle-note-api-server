package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.dto.response.Populars;
import app.bottlenote.alcohols.repository.custom.CustomPopularQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopularQueryRepository extends JpaRepository<Alcohol, Long>, CustomPopularQueryRepository {
	@Query(
		"""
			SELECT new app.bottlenote.alcohols.dto.response.Populars(
					a.id,
			       a.korName,
			       a.engName,
					COALESCE(CAST(ROUND(AVG(r1_0.ratingPoint.rating), 2) AS double), 0.0),
			       count(r1_0.ratingPoint.rating),
			       a.korCategory,
			       a.engCategory,
			       a.imageUrl,
			       case when r1_0.id.userId = :userId	then true else false end)
			       FROM alcohol a
			         LEFT JOIN rating r1_0 ON a.id = r1_0.id.alcoholId
			         JOIN (SELECT p.alcoholId, p.popularScore
			               FROM popular_alcohol p
			                        JOIN (SELECT alcoholId, MAX(createdAt) AS recent_created_at
			                              FROM popular_alcohol
			                              GROUP BY alcoholId) latest
			                             ON p.alcoholId = latest.alcoholId AND p.createdAt = latest.recent_created_at) pa
			              ON a.id = pa.alcoholId
			GROUP BY a.id, a.korName, a.engName, a.korCategory, a.engCategory, a.imageUrl,pa.popularScore
			order by pa.popularScore desc
			limit :size
			"""
	)
	List<Populars> getPopularOfWeeks(Long userId, Integer size);
}
