package app.bottlenote.banner.repository;

import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.domain.BannerRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaBannerRepository
    extends BannerRepository, JpaRepository<Banner, Long>, CustomBannerRepository {

  List<Banner> findAllByIsActiveTrue();

  boolean existsByName(String name);

  List<Banner> findAllBySortOrderGreaterThanEqual(Integer sortOrder);
}
