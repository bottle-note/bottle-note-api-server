package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DistilleryRepository {

  Page<AdminDistilleryItem> findAllDistilleries(String keyword, Pageable pageable);
}
