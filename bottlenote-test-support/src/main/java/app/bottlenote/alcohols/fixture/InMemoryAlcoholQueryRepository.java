package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.ExploreStandardCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholBulkReferenceItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.alcohols.repository.CustomAlcoholQueryRepository.AdminAlcoholDetailProjection;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryAlcoholQueryRepository implements AlcoholQueryRepository {

  private final Map<Long, Alcohol> alcohols = new HashMap<>();
  private final AtomicInteger findAllLookupItemsCount = new AtomicInteger();
  private Duration findAllLookupItemsDelay = Duration.ZERO;
  private RuntimeException findAllLookupItemsFailure;

  @Override
  public Alcohol save(Alcohol alcohol) {
    Long id = alcohol.getId();
    if (Objects.isNull(id)) {
      id = (long) (alcohols.size() + 1);
      ReflectionTestUtils.setField(alcohol, "id", id);
    }
    alcohols.put(id, alcohol);
    return alcohol;
  }

  public int findAllLookupItemsCount() {
    return findAllLookupItemsCount.get();
  }

  public void delayLookupItems(Duration delay) {
    this.findAllLookupItemsDelay = delay == null ? Duration.ZERO : delay;
  }

  public void failLookupItems(RuntimeException failure) {
    this.findAllLookupItemsFailure = failure;
  }

  @Override
  public Optional<Alcohol> findById(Long alcoholId) {
    return Optional.ofNullable(alcohols.get(alcoholId));
  }

  @Override
  public List<Alcohol> findAll() {
    return List.copyOf(alcohols.values());
  }

  @Override
  public List<Alcohol> findAllByIdIn(List<Long> ids) {
    return alcohols.values().stream().filter(alcohol -> ids.contains(alcohol.getId())).toList();
  }

  @Override
  public AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long AlcoholId) {
    return null;
  }

  @Override
  public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId) {
    return Optional.empty();
  }

  @Override
  public List<CategoryItem> findAllCategories(AlcoholType type) {
    return List.of();
  }

  @Override
  public List<CategoryItem> findAllCategoryItems() {
    return alcohols.values().stream()
        .map(a -> new CategoryItem(a.getKorCategory(), a.getEngCategory(), a.getCategoryGroup()))
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public List<AlcoholLookupItem> findAllLookupItems() {
    findAllLookupItemsCount.incrementAndGet();
    if (!findAllLookupItemsDelay.isZero() && !findAllLookupItemsDelay.isNegative()) {
      try {
        Thread.sleep(findAllLookupItemsDelay.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
    if (findAllLookupItemsFailure != null) {
      throw findAllLookupItemsFailure;
    }
    return alcohols.values().stream()
        .filter(alcohol -> alcohol.getDeletedAt() == null)
        .map(
            alcohol ->
                new AlcoholLookupItem(
                    alcohol.getId(),
                    alcohol.getKorName(),
                    alcohol.getEngName(),
                    alcohol.getKorCategory(),
                    alcohol.getEngCategory(),
                    alcohol.getCategoryGroup(),
                    alcohol.getRegion() != null ? alcohol.getRegion().getId() : null,
                    alcohol.getRegion() != null ? alcohol.getRegion().getKorName() : null,
                    alcohol.getRegion() != null ? alcohol.getRegion().getEngName() : null,
                    alcohol.getDistillery() != null ? alcohol.getDistillery().getId() : null,
                    alcohol.getDistillery() != null ? alcohol.getDistillery().getKorName() : null,
                    alcohol.getDistillery() != null ? alcohol.getDistillery().getEngName() : null,
                    alcohol.getImageUrl()))
        .toList();
  }

  @Override
  public List<AlcoholBulkReferenceItem> findAllBulkReferenceItems() {
    return alcohols.values().stream()
        .filter(a -> !a.isDeleted())
        .sorted(Comparator.comparing(Alcohol::getId))
        .map(
            a ->
                new AlcoholBulkReferenceItem(
                    a.getId(),
                    a.getKorName(),
                    a.getEngName(),
                    a.getKorCategory(),
                    a.getEngCategory(),
                    a.getCategoryGroup(),
                    a.getType(),
                    a.getDistillery() == null ? null : a.getDistillery().getId(),
                    a.getAbv(),
                    a.getVolume()))
        .toList();
  }

  @Override
  public List<AlcoholMatchTargetItem> findAllMatchTargets() {
    return alcohols.values().stream()
        .filter(alcohol -> alcohol.getDeletedAt() == null)
        .map(this::toMatchTargetItem)
        .toList();
  }

  @Override
  public List<AlcoholMatchTargetItem> findMatchTargetsByDistilleryIdIn(List<Long> distilleryIds) {
    if (distilleryIds == null || distilleryIds.isEmpty()) {
      return List.of();
    }
    return alcohols.values().stream()
        .filter(alcohol -> alcohol.getDeletedAt() == null)
        .filter(alcohol -> alcohol.getDistillery() != null)
        .filter(alcohol -> distilleryIds.contains(alcohol.getDistillery().getId()))
        .map(this::toMatchTargetItem)
        .toList();
  }

  @Override
  public List<AlcoholMatchTargetItem> findMatchTargetsByIdIn(List<Long> alcoholIds) {
    return alcohols.values().stream()
        .filter(alcohol -> alcoholIds.contains(alcohol.getId()))
        .filter(alcohol -> alcohol.getDeletedAt() == null)
        .map(this::toMatchTargetItem)
        .toList();
  }

  private AlcoholMatchTargetItem toMatchTargetItem(Alcohol alcohol) {
    return new AlcoholMatchTargetItem(
        alcohol.getId(),
        alcohol.getKorName(),
        alcohol.getEngName(),
        alcohol.getAbv(),
        alcohol.getAge(),
        alcohol.getKorCategory(),
        alcohol.getEngCategory(),
        alcohol.getRegion() != null ? alcohol.getRegion().getId() : null,
        alcohol.getRegion() != null ? alcohol.getRegion().getKorName() : null,
        alcohol.getRegion() != null ? alcohol.getRegion().getEngName() : null,
        alcohol.getDistillery() != null ? alcohol.getDistillery().getId() : null,
        alcohol.getDistillery() != null ? alcohol.getDistillery().getKorName() : null,
        alcohol.getDistillery() != null ? alcohol.getDistillery().getEngName() : null,
        alcohol.getImageUrl(),
        alcohol.getVolume());
  }

  @Override
  public Boolean existsByAlcoholId(Long alcoholId) {
    Alcohol found = alcohols.get(alcoholId);
    return found != null && found.getDeletedAt() == null;
  }

  @Override
  public Boolean existsByDistilleryId(Long distilleryId) {
    return alcohols.values().stream()
        .anyMatch(
            a ->
                a.getDistillery() != null
                    && Objects.equals(a.getDistillery().getId(), distilleryId));
  }

  @Override
  public KeysetPageResponse<List<AlcoholDetailItem>> getStandardExplore(
      ExploreStandardCriteria criteria) {
    return KeysetPageResponse.of(
        List.of(), new KeysetPagination(false, null));
  }

  @Override
  public Page<AdminAlcoholItem> searchAdminAlcohols(AdminAlcoholSearchRequest request) {
    return new PageImpl<>(List.of());
  }

  @Override
  public Optional<AdminAlcoholDetailProjection> findAdminAlcoholDetailById(Long alcoholId) {
    return Optional.empty();
  }
}
