package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.CurationKeyword;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCurationKeywordRepository
    extends JpaRepository<CurationKeyword, Long>, CustomCurationKeywordRepository {

  Optional<CurationKeyword> findByNameContainingAndIsActiveTrue(String name);
}
