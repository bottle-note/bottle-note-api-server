package app.bottlenote.banner.fixture;

import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.domain.BannerRepository;
import app.bottlenote.banner.dto.request.AdminBannerSearchRequest;
import app.bottlenote.banner.dto.response.AdminBannerListResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryBannerRepository implements BannerRepository {

  private static final Logger log = LogManager.getLogger(InMemoryBannerRepository.class);
  private final Map<Long, Banner> database = new HashMap<>();

  @Override
  public Optional<Banner> findById(Long id) {
    log.info("[InMemory] banner repository findById = {}", id);
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<Banner> findAllByIsActiveTrue() {
    List<Banner> result =
        database.values().stream()
            .filter(banner -> Boolean.TRUE.equals(banner.getIsActive()))
            .toList();

    log.info("[InMemory] banner repository findAllByIsActiveTrue = {}", result.size());
    return result;
  }

  @Override
  public Banner save(Banner banner) {
    Long id = (Long) ReflectionTestUtils.getField(banner, "id");
    if (id != null && database.containsKey(id)) {
      database.put(id, banner);
    } else {
      id = database.size() + 1L;
      database.put(id, banner);
      ReflectionTestUtils.setField(banner, "id", id);
    }
    log.info("[InMemory] banner repository save = {}", banner.getName());
    return banner;
  }

  @Override
  public void delete(Banner banner) {
    Long id = banner.getId();
    database.remove(id);
    log.info("[InMemory] banner repository delete = {}", banner.getName());
  }

  @Override
  public boolean existsByName(String name) {
    return database.values().stream().anyMatch(banner -> banner.getName().equals(name));
  }

  @Override
  public List<Banner> findAllBySortOrderGreaterThanEqual(Integer sortOrder) {
    return database.values().stream().filter(banner -> banner.getSortOrder() >= sortOrder).toList();
  }

  @Override
  public Page<AdminBannerListResponse> searchForAdmin(
      AdminBannerSearchRequest request, Pageable pageable) {
    List<AdminBannerListResponse> all =
        database.values().stream()
            .filter(
                b ->
                    request.keyword() == null
                        || request.keyword().isBlank()
                        || b.getName().contains(request.keyword()))
            .filter(b -> request.isActive() == null || b.getIsActive().equals(request.isActive()))
            .filter(
                b -> request.bannerType() == null || b.getBannerType().equals(request.bannerType()))
            .map(
                b ->
                    new AdminBannerListResponse(
                        b.getId(),
                        b.getName(),
                        b.getBannerType(),
                        b.getSortOrder(),
                        b.getIsActive(),
                        b.getStartDate(),
                        b.getEndDate(),
                        b.getCreateAt()))
            .toList();

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), all.size());
    List<AdminBannerListResponse> content =
        start < all.size() ? all.subList(start, end) : List.of();
    return new PageImpl<>(content, pageable, all.size());
  }

  public List<Banner> findAll() {
    return List.copyOf(database.values());
  }

  public void clear() {
    database.clear();
    log.info("[InMemory] banner repository cleared");
  }
}
