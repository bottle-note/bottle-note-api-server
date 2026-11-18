package app.bottlenote.picks.domain;

import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.dto.response.AlcoholPicksCountResponse;
import java.util.List;
import java.util.Optional;

public interface PicksRepository {

  Optional<Picks> findByAlcoholIdAndUserId(Long alcoholId, Long userId);

  boolean existsByAlcoholIdAndUserIdAndStatus(Long alcoholId, Long userId, PicksStatus status);

  Long countByAlcoholIdAndStatus(Long alcoholId, PicksStatus status);

  List<AlcoholPicksCountResponse> countByAlcoholIdsAndStatus(
      List<Long> alcoholIds, PicksStatus status);

  Picks save(Picks picks);
}
