package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.DISTILLERY_DUPLICATE_NAME;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.DISTILLERY_HAS_ALCOHOLS;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.DISTILLERY_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.DISTILLERY_REORDER_DUPLICATE_ID;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.DISTILLERY_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.DISTILLERY_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.DISTILLERY_SORT_ORDER_UPDATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.DISTILLERY_UPDATED;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.dto.request.AdminDistillerySortOrderRequest;
import app.bottlenote.alcohols.dto.request.AdminDistilleryUpsertRequest;
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import app.bottlenote.alcohols.exception.AlcoholException;
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
public class DistilleryService {

  private final DistilleryRepository distilleryRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

  @Transactional(readOnly = true)
  public AdminDistilleryItem getDetail(Long distilleryId) {
    Distillery distillery =
        distilleryRepository
            .findById(distilleryId)
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));
    return toAdminDistilleryItem(distillery);
  }

  @Transactional
  public AdminResultResponse create(AdminDistilleryUpsertRequest request) {
    if (distilleryRepository.existsByKorName(request.korName())
        || distilleryRepository.existsByEngName(request.engName())) {
      throw new AlcoholException(DISTILLERY_DUPLICATE_NAME);
    }

    reorderSortOrders(request.sortOrder(), null);

    Distillery distillery =
        Distillery.builder()
            .korName(request.korName())
            .engName(request.engName())
            .imageUrl(request.imageUrl())
            .sortOrder(request.sortOrder())
            .build();

    Distillery saved = distilleryRepository.save(distillery);
    return AdminResultResponse.of(DISTILLERY_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse update(Long distilleryId, AdminDistilleryUpsertRequest request) {
    Distillery distillery =
        distilleryRepository
            .findById(distilleryId)
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));

    if (distilleryRepository.existsByKorNameAndIdNot(request.korName(), distilleryId)
        || distilleryRepository.existsByEngNameAndIdNot(request.engName(), distilleryId)) {
      throw new AlcoholException(DISTILLERY_DUPLICATE_NAME);
    }

    if (!distillery.getSortOrder().equals(request.sortOrder())) {
      reorderSortOrders(request.sortOrder(), distilleryId);
    }

    distillery.update(
        request.korName(), request.engName(), request.imageUrl(), request.sortOrder());

    return AdminResultResponse.of(DISTILLERY_UPDATED, distilleryId);
  }

  @Transactional
  public AdminResultResponse delete(Long distilleryId) {
    Distillery distillery =
        distilleryRepository
            .findById(distilleryId)
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));

    if (Boolean.TRUE.equals(alcoholQueryRepository.existsByDistilleryId(distilleryId))) {
      throw new AlcoholException(DISTILLERY_HAS_ALCOHOLS);
    }

    distilleryRepository.delete(distillery);
    return AdminResultResponse.of(DISTILLERY_DELETED, distilleryId);
  }

  @Transactional
  public AdminResultResponse updateSortOrder(
      Long distilleryId, AdminDistillerySortOrderRequest request) {
    Distillery distillery =
        distilleryRepository
            .findById(distilleryId)
            .orElseThrow(() -> new AlcoholException(DISTILLERY_NOT_FOUND));

    if (!distillery.getSortOrder().equals(request.sortOrder())) {
      reorderSortOrders(request.sortOrder(), distilleryId);
    }

    distillery.updateSortOrder(request.sortOrder());
    return AdminResultResponse.of(DISTILLERY_SORT_ORDER_UPDATED, distilleryId);
  }

  @Transactional
  public AdminResultResponse reorder(AdminBulkReorderRequest request) {
    validateReorderIds(request.ids());

    List<Distillery> orderedDistilleries =
        distilleryRepository.findAllOrderBySortOrderAsc().stream()
            .filter(distillery -> !Long.valueOf(0L).equals(distillery.getId()))
            .toList();
    Map<Long, Distillery> distilleryById =
        orderedDistilleries.stream()
            .collect(Collectors.toMap(Distillery::getId, Function.identity()));

    Set<Long> requestedIds = new HashSet<>(request.ids());
    List<Distillery> reordered =
        new ArrayList<>(
            request.ids().stream().map(id -> findReorderTarget(distilleryById, id)).toList());
    reordered.addAll(
        orderedDistilleries.stream()
            .filter(distillery -> !requestedIds.contains(distillery.getId()))
            .toList());

    for (int i = 0; i < reordered.size(); i++) {
      reordered.get(i).updateSortOrder(i + 1);
    }
    return AdminResultResponse.of(DISTILLERY_SORT_ORDER_UPDATED, null);
  }

  private void reorderSortOrders(Integer newSortOrder, Long excludeDistilleryId) {
    if (newSortOrder == null) {
      return;
    }
    List<Distillery> conflicting =
        distilleryRepository.findAllBySortOrderGreaterThanEqual(newSortOrder);
    conflicting.stream()
        .filter(d -> excludeDistilleryId == null || !d.getId().equals(excludeDistilleryId))
        .forEach(d -> d.updateSortOrder(d.getSortOrder() + 1));
  }

  private void validateReorderIds(List<Long> ids) {
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new AlcoholException(DISTILLERY_REORDER_DUPLICATE_ID);
    }
  }

  private Distillery findReorderTarget(Map<Long, Distillery> distilleryById, Long distilleryId) {
    Distillery distillery = distilleryById.get(distilleryId);
    if (distillery == null) {
      throw new AlcoholException(DISTILLERY_NOT_FOUND);
    }
    return distillery;
  }

  private AdminDistilleryItem toAdminDistilleryItem(Distillery d) {
    return new AdminDistilleryItem(
        d.getId(),
        d.getKorName(),
        d.getEngName(),
        d.getImageUrl(),
        d.getCreateAt(),
        d.getLastModifyAt(),
        d.getSortOrder());
  }
}
