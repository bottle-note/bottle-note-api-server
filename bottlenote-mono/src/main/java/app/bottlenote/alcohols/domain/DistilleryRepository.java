package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import java.util.List;

public interface DistilleryRepository {

  List<AdminDistilleryItem> findAllDistilleries();
}
