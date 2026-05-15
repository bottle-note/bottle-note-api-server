package app.bottlenote.curation.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@DomainRepository
public interface CurationRepository {

  Optional<Curation> findById(Long id);

  List<Curation> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();

  Page<Curation> searchForAdmin(String keyword, Boolean isActive, Pageable pageable);

  Curation save(Curation curation);

  void delete(Curation curation);
}
