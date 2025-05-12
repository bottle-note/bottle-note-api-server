package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import feign.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@JpaRepositoryImpl
public interface JpaPopularQueryRepository extends JpaRepository<Alcohol, Long> {

	@Query("""
			         SELECT
			         new app.bottlenote.alcohols.dto.response.PopularItem(
					        a1_0.id,
			               a1_0.korName,
			               a1_0.engName,
			               CAST(ROUND(AVG(r1_0.ratingPoint.rating), 2) AS double),
			               count(r1_0.ratingPoint.rating),
			               a1_0.korCategory,
			               a1_0.engCategory,
			               a1_0.imageUrl,
			                          CASE
			                              WHEN (SELECT COUNT(p) FROM picks p WHERE p.userId = :userId AND p.alcoholId = a1_0.id) > 0 THEN true
			                              ELSE false
			                          END,
			               cast( p1_0.popularScore as double)
						)
			        from popular_alcohol p1_0
			                 join alcohol a1_0 on p1_0.alcoholId = a1_0.id
			                 left join rating r1_0 on a1_0.id = r1_0.id.alcoholId
			        WHERE p1_0.createdAt = (SELECT MAX(p2.createdAt)
			                              FROM popular_alcohol p2
			                              WHERE p2.alcoholId = p1_0.alcoholId)
			        group by p1_0.id,p1_0.popularScore
			        order by p1_0.popularScore desc
			""")
	List<PopularItem> getPopularOfWeeks(@Param("userId") Long userId, Pageable size);

	@Query("""
			                  SELECT new app.bottlenote.alcohols.dto.response.PopularItem(
			                       a1_0.id,
			                        a1_0.korName,
			                        a1_0.engName,
			                        CAST(ROUND(AVG(r1_0.ratingPoint.rating), 2) AS double),
			                        count(r1_0.ratingPoint.rating),
			                        a1_0.korCategory,
			                        a1_0.engCategory,
			                        a1_0.imageUrl,
			                                   CASE
			                                       WHEN (SELECT COUNT(p) FROM picks p WHERE p.userId = :userId AND p.alcoholId = a1_0.id) > 0 THEN true
			                                       ELSE false
			                                   END,
			                 COALESCE((SELECT CAST(MAX(pa.popularScore) AS double) FROM popular_alcohol pa WHERE pa.alcoholId = a1_0.id), 0.0)
			                  )
			                 from alcohol a1_0
			              join alcohol_tasting_tags  at1_0 on a1_0.id = at1_0.alcohol.id
			              left join rating r1_0 on a1_0.id = r1_0.id.alcoholId
			                 WHERE at1_0.tastingTag.id in (:tags)
			                 GROUP BY a1_0.id, a1_0.korName, a1_0.engName, a1_0.korCategory, a1_0.engCategory, a1_0.imageUrl
			ORDER BY FUNCTION('RAND')
			""")
	List<PopularItem> getSpringItems(
			@Param("userId") Long userId,
			@Param("tags") List<Long> tags,
			Pageable size);
}
