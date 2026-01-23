package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaDistilleryRepository
    extends DistilleryRepository, CrudRepository<Distillery, Long> {

  @Override
  @Query(
      """
      select new app.bottlenote.alcohols.dto.response.AdminDistilleryItem(
        d.id, d.korName, d.engName, d.logoImgPath, d.createAt, d.lastModifyAt
      )
      from distillery d
      where (:keyword is null or :keyword = ''
        or d.korName like concat('%', :keyword, '%')
        or d.engName like concat('%', :keyword, '%'))
      """)
  Page<AdminDistilleryItem> findAllDistilleries(
      @Param("keyword") String keyword, Pageable pageable);
}
