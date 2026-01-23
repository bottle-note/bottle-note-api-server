package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DistilleryRepository {

  Optional<Distillery> findById(Long id);

  Page<AdminDistilleryItem> findAllDistilleries(String keyword, Pageable pageable);
}
