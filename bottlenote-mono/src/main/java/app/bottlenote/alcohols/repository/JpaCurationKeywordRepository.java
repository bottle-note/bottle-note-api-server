package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.domain.CurationKeywordRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCurationKeywordRepository
    extends CurationKeywordRepository,
        JpaRepository<CurationKeyword, Long>,
        CustomCurationKeywordRepository {

  boolean existsByName(String name);
}
