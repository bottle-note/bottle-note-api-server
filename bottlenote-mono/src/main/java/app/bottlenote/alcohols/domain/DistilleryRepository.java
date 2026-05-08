package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DistilleryRepository {

  Optional<Distillery> findById(Long id);

  Page<AdminDistilleryItem> findAllDistilleries(String keyword, Pageable pageable);

  Distillery save(Distillery distillery);

  void delete(Distillery distillery);

  boolean existsByKorName(String korName);

  boolean existsByEngName(String engName);

  boolean existsByKorNameAndIdNot(String korName, Long id);

  boolean existsByEngNameAndIdNot(String engName, Long id);

  List<Distillery> findAllBySortOrderGreaterThanEqual(int sortOrder);
}
