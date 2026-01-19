package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

@JpaRepositoryImpl
public interface JpaDistilleryRepository
    extends DistilleryRepository, CrudRepository<Distillery, Long> {

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AdminDistilleryItem(
        d.id, d.korName, d.engName, d.logoImgPath, d.createAt, d.lastModifyAt
      )
      from distillery d order by d.id asc
      """)
  List<AdminDistilleryItem> findAllDistilleries();
}
