package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.domain.CurationKeywordRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCurationKeywordRepository
    extends CurationKeywordRepository,
        JpaRepository<CurationKeyword, Long>,
        CustomCurationKeywordRepository {

  boolean existsByName(String name);

  @Override
  @Query("select c from curation_keyword c order by c.displayOrder asc, c.id asc")
  List<CurationKeyword> findAllOrderByDisplayOrderAsc();
}
