package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Region;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class RegionTestFactory {

  @PersistenceContext private EntityManager em;

  @Transactional
  @NotNull
  public Region persistRegion(
      @NotNull String korName, @NotNull String engName, Region parent, int sortOrder) {
    Region region =
        Region.builder()
            .korName(korName)
            .engName(engName)
            .continent("Europe")
            .description(korName + " 설명")
            .sortOrder(sortOrder)
            .parent(parent)
            .build();
    em.persist(region);
    em.flush();
    return region;
  }

  @Transactional
  @NotNull
  public Region persistRoot(@NotNull String korName, @NotNull String engName, int sortOrder) {
    return persistRegion(korName, engName, null, sortOrder);
  }
}
