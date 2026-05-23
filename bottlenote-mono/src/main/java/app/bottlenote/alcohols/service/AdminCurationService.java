package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.CURATION_ALCOHOL_NOT_INCLUDED;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.CURATION_DUPLICATE_NAME;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.CURATION_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.CURATION_REORDER_DUPLICATE_ID;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_ALCOHOL_ADDED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_ALCOHOL_REMOVED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_DISPLAY_ORDER_UPDATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_STATUS_UPDATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CURATION_UPDATED;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.domain.CurationKeywordRepository;
import app.bottlenote.alcohols.dto.request.AdminCurationAlcoholRequest;
import app.bottlenote.alcohols.dto.request.AdminCurationCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminCurationDisplayOrderRequest;
import app.bottlenote.alcohols.dto.request.AdminCurationSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminCurationStatusRequest;
import app.bottlenote.alcohols.dto.request.AdminCurationUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AdminCurationDetailResponse;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.data.response.GlobalResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCurationService {

  private final CurationKeywordRepository curationKeywordRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

  @Transactional(readOnly = true)
  public GlobalResponse search(AdminCurationSearchRequest request) {
    PageRequest pageable = PageRequest.of(request.page(), request.size());
    return GlobalResponse.fromPage(curationKeywordRepository.searchForAdmin(request, pageable));
  }

  @Transactional(readOnly = true)
  public AdminCurationDetailResponse getDetail(Long curationId) {
    CurationKeyword curation =
        curationKeywordRepository
            .findById(curationId)
            .orElseThrow(() -> new AlcoholException(CURATION_NOT_FOUND));

    List<AdminAlcoholItem> alcohols =
        alcoholQueryRepository.findAllByIdIn(new ArrayList<>(curation.getAlcoholIds())).stream()
            .map(
                alcohol ->
                    new AdminAlcoholItem(
                        alcohol.getId(),
                        alcohol.getKorName(),
                        alcohol.getEngName(),
                        alcohol.getKorCategory(),
                        alcohol.getEngCategory(),
                        alcohol.getImageUrl(),
                        alcohol.getCreateAt(),
                        alcohol.getLastModifyAt(),
                        alcohol.getDeletedAt()))
            .toList();

    return AdminCurationDetailResponse.of(
        curation.getId(),
        curation.getName(),
        curation.getDescription(),
        curation.getCoverImageUrl(),
        curation.getDisplayOrder(),
        curation.getIsActive(),
        alcohols,
        curation.getCreateAt(),
        curation.getLastModifyAt());
  }

  @Transactional
  public AdminResultResponse create(AdminCurationCreateRequest request) {
    if (curationKeywordRepository.existsByName(request.name())) {
      throw new AlcoholException(CURATION_DUPLICATE_NAME);
    }

    CurationKeyword curation =
        CurationKeyword.create(
            request.name(),
            request.description(),
            request.coverImageUrl(),
            request.displayOrder(),
            new HashSet<>(request.alcoholIds()));

    CurationKeyword saved = curationKeywordRepository.save(curation);
    return AdminResultResponse.of(CURATION_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse update(Long curationId, AdminCurationUpdateRequest request) {
    CurationKeyword curation =
        curationKeywordRepository
            .findById(curationId)
            .orElseThrow(() -> new AlcoholException(CURATION_NOT_FOUND));

    curation.update(
        request.name(),
        request.description(),
        request.coverImageUrl(),
        request.displayOrder(),
        request.isActive(),
        new HashSet<>(request.alcoholIds()));

    return AdminResultResponse.of(CURATION_UPDATED, curationId);
  }

  @Transactional
  public AdminResultResponse delete(Long curationId) {
    CurationKeyword curation =
        curationKeywordRepository
            .findById(curationId)
            .orElseThrow(() -> new AlcoholException(CURATION_NOT_FOUND));

    curationKeywordRepository.delete(curation);
    return AdminResultResponse.of(CURATION_DELETED, curationId);
  }

  @Transactional
  public AdminResultResponse updateStatus(Long curationId, AdminCurationStatusRequest request) {
    CurationKeyword curation =
        curationKeywordRepository
            .findById(curationId)
            .orElseThrow(() -> new AlcoholException(CURATION_NOT_FOUND));

    curation.updateStatus(request.isActive());
    return AdminResultResponse.of(CURATION_STATUS_UPDATED, curationId);
  }

  @Transactional
  public AdminResultResponse updateDisplayOrder(
      Long curationId, AdminCurationDisplayOrderRequest request) {
    CurationKeyword curation =
        curationKeywordRepository
            .findById(curationId)
            .orElseThrow(() -> new AlcoholException(CURATION_NOT_FOUND));

    curation.updateDisplayOrder(request.displayOrder());
    return AdminResultResponse.of(CURATION_DISPLAY_ORDER_UPDATED, curationId);
  }

  @Transactional
  public AdminResultResponse reorder(AdminBulkReorderRequest request) {
    validateReorderIds(request.ids());

    List<CurationKeyword> orderedCurations =
        curationKeywordRepository.findAllOrderByDisplayOrderAsc();
    Map<Long, CurationKeyword> curationById =
        orderedCurations.stream()
            .collect(Collectors.toMap(CurationKeyword::getId, Function.identity()));

    Set<Long> requestedIds = new HashSet<>(request.ids());
    List<CurationKeyword> reordered =
        new ArrayList<>(
            request.ids().stream().map(id -> findReorderTarget(curationById, id)).toList());
    reordered.addAll(
        orderedCurations.stream()
            .filter(curation -> !requestedIds.contains(curation.getId()))
            .toList());

    List<Integer> slots =
        orderedCurations.stream().map(CurationKeyword::getDisplayOrder).sorted().toList();
    for (int i = 0; i < reordered.size(); i++) {
      reordered.get(i).updateDisplayOrder(slots.get(i));
    }
    return AdminResultResponse.of(CURATION_DISPLAY_ORDER_UPDATED, null);
  }

  @Transactional
  public AdminResultResponse addAlcohols(Long curationId, AdminCurationAlcoholRequest request) {
    CurationKeyword curation =
        curationKeywordRepository
            .findById(curationId)
            .orElseThrow(() -> new AlcoholException(CURATION_NOT_FOUND));

    curation.addAlcohols(request.alcoholIds());
    return AdminResultResponse.of(CURATION_ALCOHOL_ADDED, curationId);
  }

  @Transactional
  public AdminResultResponse removeAlcohol(Long curationId, Long alcoholId) {
    CurationKeyword curation =
        curationKeywordRepository
            .findById(curationId)
            .orElseThrow(() -> new AlcoholException(CURATION_NOT_FOUND));

    if (!curation.getAlcoholIds().contains(alcoholId)) {
      throw new AlcoholException(CURATION_ALCOHOL_NOT_INCLUDED);
    }

    curation.removeAlcohol(alcoholId);
    return AdminResultResponse.of(CURATION_ALCOHOL_REMOVED, curationId);
  }

  private void validateReorderIds(List<Long> ids) {
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new AlcoholException(CURATION_REORDER_DUPLICATE_ID);
    }
  }

  private CurationKeyword findReorderTarget(
      Map<Long, CurationKeyword> curationById, Long curationId) {
    CurationKeyword curation = curationById.get(curationId);
    if (curation == null) {
      throw new AlcoholException(CURATION_NOT_FOUND);
    }
    return curation;
  }
}
