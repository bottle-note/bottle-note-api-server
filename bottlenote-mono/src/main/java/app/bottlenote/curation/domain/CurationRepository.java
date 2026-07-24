package app.bottlenote.curation.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.curation.dto.dsl.CurationFeedSearchCriteria;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@DomainRepository
public interface CurationRepository {

  Optional<Curation> findById(Long id);

  List<Curation> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();

  List<Curation> findAllVisibleOn(LocalDate today);

  List<Long> findFeedCandidateIds(CurationFeedSearchCriteria criteria);

  List<Curation> findAllByIdIn(Collection<Long> ids);

  Optional<Curation> findVisibleById(Long id, LocalDate today);

  Page<Curation> searchForAdmin(String keyword, Long specId, Boolean isActive, Pageable pageable);

  Curation save(Curation curation);

  void delete(Curation curation);
}
