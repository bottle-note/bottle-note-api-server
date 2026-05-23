package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_DUPLICATE_ENG_NAME;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_DUPLICATE_KOR_NAME;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_HAS_ALCOHOLS;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_HAS_CHILDREN;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_MAX_DEPTH_EXCEEDED;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_PARENT_CYCLE;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_PARENT_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_REORDER_DUPLICATE_ID;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.REGION_REORDER_SCOPE_MISMATCH;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.REGION_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.REGION_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.REGION_SORT_ORDER_UPDATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.REGION_UPDATED;

import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.RegionRepository;
import app.bottlenote.alcohols.dto.request.AdminRegionCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminRegionSortOrderRequest;
import app.bottlenote.alcohols.dto.request.AdminRegionUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminRegionDetailResponse;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.global.dto.request.AdminBulkReorderRequest;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRegionService {

  private final RegionRepository regionRepository;

  @Transactional(readOnly = true)
  public AdminRegionDetailResponse getDetail(Long regionId) {
    Region region =
        regionRepository
            .findById(regionId)
            .orElseThrow(() -> new AlcoholException(REGION_NOT_FOUND));

    Region parent = region.getParent();
    boolean hasChildren = !regionRepository.findChildRegionIds(regionId).isEmpty();
    long alcoholCount = regionRepository.countAlcoholsByRegionId(regionId);

    return new AdminRegionDetailResponse(
        region.getId(),
        region.getKorName(),
        region.getEngName(),
        region.getContinent(),
        region.getDescription(),
        region.getSortOrder(),
        parent != null ? parent.getId() : null,
        parent != null ? parent.getKorName() : null,
        hasChildren,
        alcoholCount,
        region.getCreateAt(),
        region.getLastModifyAt());
  }

  @Transactional
  public AdminResultResponse create(AdminRegionCreateRequest request) {
    validateUniqueNames(request.korName(), request.engName(), null);

    Region parent = resolveParent(request.parentId(), null);

    reorderSortOrders(request.sortOrder(), null);

    Region region =
        Region.builder()
            .korName(request.korName())
            .engName(request.engName())
            .continent(request.continent())
            .description(request.description())
            .sortOrder(request.sortOrder())
            .parent(parent)
            .build();

    Region saved = regionRepository.save(region);
    return AdminResultResponse.of(REGION_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse update(Long regionId, AdminRegionUpdateRequest request) {
    Region region =
        regionRepository
            .findById(regionId)
            .orElseThrow(() -> new AlcoholException(REGION_NOT_FOUND));

    validateUniqueNames(request.korName(), request.engName(), regionId);

    Region parent = resolveParent(request.parentId(), regionId);

    if (!region.getSortOrder().equals(request.sortOrder())) {
      reorderSortOrders(request.sortOrder(), regionId);
    }

    region.update(
        request.korName(),
        request.engName(),
        request.continent(),
        request.description(),
        request.sortOrder(),
        parent);

    return AdminResultResponse.of(REGION_UPDATED, regionId);
  }

  @Transactional
  public AdminResultResponse delete(Long regionId) {
    Region region =
        regionRepository
            .findById(regionId)
            .orElseThrow(() -> new AlcoholException(REGION_NOT_FOUND));

    if (!regionRepository.findChildRegionIds(regionId).isEmpty()) {
      throw new AlcoholException(REGION_HAS_CHILDREN);
    }

    if (regionRepository.existsAlcoholByRegionId(regionId)) {
      throw new AlcoholException(REGION_HAS_ALCOHOLS);
    }

    regionRepository.delete(region);
    return AdminResultResponse.of(REGION_DELETED, regionId);
  }

  @Transactional
  public AdminResultResponse updateSortOrder(Long regionId, AdminRegionSortOrderRequest request) {
    Region region =
        regionRepository
            .findById(regionId)
            .orElseThrow(() -> new AlcoholException(REGION_NOT_FOUND));

    if (!region.getSortOrder().equals(request.sortOrder())) {
      reorderSortOrders(request.sortOrder(), regionId);
    }

    region.updateSortOrder(request.sortOrder());
    return AdminResultResponse.of(REGION_SORT_ORDER_UPDATED, regionId);
  }

  @Transactional
  public AdminResultResponse reorder(AdminBulkReorderRequest request) {
    validateReorderIds(request.ids());
    reorderInScope(request.ids(), regionRepository.findAllOrderBySortOrderAsc(), REGION_NOT_FOUND);
    return AdminResultResponse.of(REGION_SORT_ORDER_UPDATED, null);
  }

  @Transactional
  public AdminResultResponse reorderChildren(Long parentId, AdminBulkReorderRequest request) {
    validateReorderIds(request.ids());
    regionRepository
        .findById(parentId)
        .orElseThrow(() -> new AlcoholException(REGION_PARENT_NOT_FOUND));

    reorderInScope(
        request.ids(),
        regionRepository.findAllByParentIdOrderBySortOrderAsc(parentId),
        REGION_REORDER_SCOPE_MISMATCH);
    return AdminResultResponse.of(REGION_SORT_ORDER_UPDATED, parentId);
  }

  private void validateUniqueNames(String korName, String engName, Long excludeId) {
    boolean korDuplicated =
        excludeId == null
            ? regionRepository.existsByKorName(korName)
            : regionRepository.existsByKorNameAndIdNot(korName, excludeId);
    if (korDuplicated) {
      throw new AlcoholException(REGION_DUPLICATE_KOR_NAME);
    }

    boolean engDuplicated =
        excludeId == null
            ? regionRepository.existsByEngName(engName)
            : regionRepository.existsByEngNameAndIdNot(engName, excludeId);
    if (engDuplicated) {
      throw new AlcoholException(REGION_DUPLICATE_ENG_NAME);
    }
  }

  private Region resolveParent(Long parentId, Long selfId) {
    if (parentId == null) {
      return null;
    }
    if (selfId != null && parentId.equals(selfId)) {
      throw new AlcoholException(REGION_PARENT_CYCLE);
    }

    Region parent =
        regionRepository
            .findById(parentId)
            .orElseThrow(() -> new AlcoholException(REGION_PARENT_NOT_FOUND));

    if (parent.getParent() != null) {
      throw new AlcoholException(REGION_MAX_DEPTH_EXCEEDED);
    }

    if (selfId != null) {
      List<Long> descendants = regionRepository.findChildRegionIds(selfId);
      if (descendants.contains(parentId)) {
        throw new AlcoholException(REGION_PARENT_CYCLE);
      }
      if (!descendants.isEmpty()) {
        throw new AlcoholException(REGION_MAX_DEPTH_EXCEEDED);
      }
    }

    return parent;
  }

  private void reorderSortOrders(Integer newSortOrder, Long excludeRegionId) {
    if (newSortOrder == null) {
      return;
    }
    List<Region> conflicting = regionRepository.findAllBySortOrderGreaterThanEqual(newSortOrder);
    conflicting.stream()
        .filter(r -> excludeRegionId == null || !r.getId().equals(excludeRegionId))
        .forEach(r -> r.updateSortOrder(r.getSortOrder() + 1));
  }

  private void validateReorderIds(List<Long> ids) {
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new AlcoholException(REGION_REORDER_DUPLICATE_ID);
    }
  }

  private void reorderInScope(
      List<Long> ids, List<Region> orderedRegions, AlcoholExceptionCode notFoundCode) {
    Map<Long, Region> regionById =
        orderedRegions.stream().collect(Collectors.toMap(Region::getId, Function.identity()));
    Set<Long> requestedIds = new HashSet<>(ids);
    List<Region> reordered =
        new ArrayList<>(
            ids.stream().map(id -> findReorderTarget(regionById, id, notFoundCode)).toList());
    reordered.addAll(
        orderedRegions.stream().filter(region -> !requestedIds.contains(region.getId())).toList());

    List<Integer> slots = orderedRegions.stream().map(Region::getSortOrder).sorted().toList();
    for (int i = 0; i < reordered.size(); i++) {
      reordered.get(i).updateSortOrder(slots.get(i));
    }
  }

  private Region findReorderTarget(
      Map<Long, Region> regionById, Long regionId, AlcoholExceptionCode notFoundCode) {
    Region region = regionById.get(regionId);
    if (region == null) {
      throw new AlcoholException(notFoundCode);
    }
    return region;
  }
}
