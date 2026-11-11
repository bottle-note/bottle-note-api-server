package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ALREADY_DELETED;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_MERGE_SELF;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.ALCOHOL_MERGED;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholMergeHandler;
import app.bottlenote.alcohols.domain.AlcoholMergeLog;
import app.bottlenote.alcohols.domain.AlcoholMergeRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholMergeLogSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholMergeRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergeLogResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergePreviewResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergePreviewResponse.AlcoholItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergePreviewResponse.DuplicateItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.global.pagination.OffsetPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
@RequiredArgsConstructor
public class AdminAlcoholMergeService {

  private final AlcoholMergeRepository alcoholMergeRepository;
  private final List<AlcoholMergeHandler> mergeHandlers;

  /** 병합을 실제로 실행한 뒤 롤백한다. 건수를 따로 세지 않아 미리보기와 병합의 건수가 달라지지 않는다. */
  @Transactional
  public AdminAlcoholMergePreviewResponse preview(AdminAlcoholMergeRequest request) {
    AdminAlcoholMergePreviewResponse response = execute(request);
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    return response;
  }

  @Transactional
  public AdminResultResponse merge(AdminAlcoholMergeRequest request) {
    execute(request);
    return AdminResultResponse.of(ALCOHOL_MERGED, request.baseAlcoholId());
  }

  @Transactional(readOnly = true)
  public GlobalResponse searchLogs(AdminAlcoholMergeLogSearchRequest request) {
    OffsetPage<AlcoholMergeLog> logs =
        alcoholMergeRepository.searchLogs(
            request.baseAlcoholId(), request.duplicateAlcoholId(), request.page(), request.size());
    return GlobalResponse.fromPage(
        new PageImpl<>(
            logs.content().stream().map(AdminAlcoholMergeService::toLogResponse).toList(),
            PageRequest.of(request.page(), request.size()),
            logs.totalElements()));
  }

  private AdminAlcoholMergePreviewResponse execute(AdminAlcoholMergeRequest request) {
    Long baseId = request.baseAlcoholId();
    List<Long> duplicateIds = request.duplicateAlcoholIds();
    if (duplicateIds.contains(baseId)) {
      throw new AlcoholException(ALCOHOL_MERGE_SELF);
    }

    List<Long> alcoholIds = new ArrayList<>(duplicateIds);
    alcoholIds.add(baseId);
    Map<Long, Alcohol> alcohols =
        alcoholMergeRepository.lockAlcohols(alcoholIds).stream()
            .collect(Collectors.toMap(Alcohol::getId, Function.identity()));
    if (alcohols.size() != alcoholIds.size()) {
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    }
    // 삭제된 위스키를 합치면 숨겨져 있던 데이터가 기준 위스키에 다시 노출된다
    if (alcohols.values().stream().anyMatch(Alcohol::isDeleted)) {
      throw new AlcoholException(ALCOHOL_ALREADY_DELETED);
    }
    Alcohol base = alcohols.get(baseId);

    String mergeGroupId = UUID.randomUUID().toString();
    List<DuplicateItem> duplicates = new ArrayList<>();
    for (Long duplicateId : duplicateIds) {
      Alcohol duplicate = alcohols.get(duplicateId);
      List<AlcoholMergeCount> counts =
          mergeHandlers.stream()
              .flatMap(handler -> handler.merge(duplicateId, baseId).stream())
              .toList();
      alcoholMergeRepository.save(AlcoholMergeLog.of(mergeGroupId, base, duplicate, counts));
      alcoholMergeRepository.deleteAlcohol(duplicateId);
      duplicates.add(new DuplicateItem(toAlcoholItem(duplicate), counts));
    }
    return new AdminAlcoholMergePreviewResponse(toAlcoholItem(base), duplicates);
  }

  private static AlcoholItem toAlcoholItem(Alcohol alcohol) {
    return new AlcoholItem(
        alcohol.getId(), alcohol.getKorName(), alcohol.getEngName(), alcohol.getImageUrl());
  }

  private static AdminAlcoholMergeLogResponse toLogResponse(AlcoholMergeLog log) {
    return new AdminAlcoholMergeLogResponse(
        log.getId(),
        log.getMergeGroupId(),
        log.getBaseAlcoholId(),
        log.getBaseKorName(),
        log.getDuplicateAlcoholId(),
        log.getDuplicateKorName(),
        log.getDuplicateEngName(),
        log.getCounts(),
        log.getCreatePrincipal() != null ? log.getCreatePrincipal().getId() : null,
        log.getCreatePrincipal() != null ? log.getCreatePrincipal().getEmail() : null,
        log.getCreateAt());
  }
}
