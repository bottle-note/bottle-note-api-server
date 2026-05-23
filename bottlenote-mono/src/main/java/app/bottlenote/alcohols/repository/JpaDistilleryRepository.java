package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
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
        d.id, d.korName, d.engName, d.imageUrl, d.createAt, d.lastModifyAt, d.sortOrder
      )
      from distillery d
      where (:keyword is null or :keyword = ''
        or d.korName like concat('%', :keyword, '%')
        or d.engName like concat('%', :keyword, '%'))
      order by d.sortOrder asc, d.korName asc
      """)
  Page<AdminDistilleryItem> findAllDistilleries(
      @Param("keyword") String keyword, Pageable pageable);

  @Override
  @Query("select d from distillery d where d.sortOrder >= :sortOrder")
  List<Distillery> findAllBySortOrderGreaterThanEqual(@Param("sortOrder") int sortOrder);

  @Override
  @Query("select d from distillery d order by d.sortOrder asc, d.id asc")
  List<Distillery> findAllOrderBySortOrderAsc();
}
