package app.bottlenote.support.business.fixture;

import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.repository.BusinessSupportRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryBusinessSupportRepository implements BusinessSupportRepository {
  private static final Logger log = LogManager.getLogger(InMemoryBusinessSupportRepository.class);
  private final Map<Long, BusinessSupport> database = new HashMap<>();
  private Long sequence = 1L;

  @Override
  public Optional<BusinessSupport> findTopByUserIdAndContentOrderByIdDesc(
      Long userId, String content) {
    return database.values().stream()
        .filter(bs -> bs.getUserId().equals(userId) && bs.getContent().equals(content))
        .max((bs1, bs2) -> bs1.getId().compareTo(bs2.getId()));
  }

  @Override
  public Optional<BusinessSupport> findByIdAndUserId(Long id, Long userId) {
    return database.values().stream()
        .filter(bs -> bs.getId().equals(id) && bs.getUserId().equals(userId))
        .findFirst();
  }

  @Override
  public List<BusinessSupport> findAllByUserId(Long userId) {
    return database.values().stream()
        .filter(bs -> bs.getUserId().equals(userId))
        .collect(Collectors.toList());
  }

  @Override
  public BusinessSupport save(BusinessSupport entity) {
    Long id = (Long) ReflectionTestUtils.getField(entity, "id");
    if (id == null) {
      id = sequence++;
      ReflectionTestUtils.setField(entity, "id", id);
    }
    database.put(id, entity);
    log.info("[InMemory] business support repository save = {}", entity);
    return entity;
  }

  @Override
  public Optional<BusinessSupport> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<BusinessSupport> findAll() {
    return new ArrayList<>(database.values());
  }
}
