package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.Collection;
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
      """)
  Page<AdminDistilleryItem> findAllDistilleries(
      @Param("keyword") String keyword, Pageable pageable);

  @Override
  @Query("select d from distillery d where d.sortOrder >= :sortOrder")
  List<Distillery> findAllBySortOrderGreaterThanEqual(@Param("sortOrder") int sortOrder);

  @Override
  // 일괄 재정렬이 나머지 행을 관리자 목록에 보이는 순서대로 이어 붙이도록 동순위는 이름, ID 순으로 정한다.
  @Query("select d from distillery d order by d.sortOrder asc, d.korName asc, d.id asc")
  List<Distillery> findAllOrderBySortOrderAsc();

  @Override
  @Query("select d from distillery d where d.id in :ids order by d.sortOrder asc, d.id asc")
  List<Distillery> findAllByIdInOrderBySortOrderAsc(@Param("ids") Collection<Long> ids);
}
