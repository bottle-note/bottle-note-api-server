package app.bottlenote.picks.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.domain.PicksRepository;
import app.bottlenote.picks.dto.response.AlcoholPicksCountResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaPicksRepository extends PicksRepository, JpaRepository<Picks, Long> {
  Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId);

  @Override
  @Query(
      """
      select new app.bottlenote.picks.dto.response.AlcoholPicksCountResponse(p.alcoholId, count(p))
      from picks p
      where p.alcoholId in :alcoholIds
        and p.status = :status
      group by p.alcoholId
      """)
  List<AlcoholPicksCountResponse> countByAlcoholIdsAndStatus(
      @Param("alcoholIds") List<Long> alcoholIds, @Param("status") PicksStatus status);
}
