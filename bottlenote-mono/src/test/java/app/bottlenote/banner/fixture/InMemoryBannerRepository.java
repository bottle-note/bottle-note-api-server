package app.bottlenote.banner.fixture;

import app.bottlenote.banner.domain.Banner;
import app.bottlenote.banner.domain.BannerRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

  /** 테스트용 저장 메서드 */
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

  /** 테스트용 전체 조회 메서드 */
  public List<Banner> findAll() {
    return List.copyOf(database.values());
  }

  /** 테스트용 초기화 메서드 */
  public void clear() {
    database.clear();
    log.info("[InMemory] banner repository cleared");
  }
}
