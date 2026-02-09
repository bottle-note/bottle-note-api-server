package app.bottlenote.banner.domain;

import app.bottlenote.banner.dto.request.AdminBannerSearchRequest;
import app.bottlenote.banner.dto.response.AdminBannerListResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BannerRepository {

  Optional<Banner> findById(Long id);

  List<Banner> findAllByIsActiveTrue();

  Banner save(Banner banner);

  void delete(Banner banner);

  boolean existsByName(String name);

  List<Banner> findAllBySortOrderGreaterThanEqual(Integer sortOrder);

  Page<AdminBannerListResponse> searchForAdmin(AdminBannerSearchRequest request, Pageable pageable);
}
