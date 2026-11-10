package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ALREADY_DELETED;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_MERGE_SELF;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.ALCOHOL_MERGED;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import app.bottlenote.alcohols.domain.AlcoholMergeLog;
import app.bottlenote.alcohols.domain.AlcoholMergeRepository;
import app.bottlenote.alcohols.domain.AlcoholReferenceMerger;
import app.bottlenote.alcohols.dto.request.AdminAlcoholMergeLogSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholMergeRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergeLogResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergePreviewResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergePreviewResponse.AlcoholItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergePreviewResponse.SourceItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
@RequiredArgsConstructor
public class AdminAlcoholMergeService {

  private final AlcoholMergeRepository alcoholMergeRepository;
  private final List<AlcoholReferenceMerger> referenceMergers;

  /** 병합을 실제로 실행한 뒤 롤백한다. 미리보기 건수와 병합 결과가 같은 코드에서 나온다. */
  @Transactional
  public AdminAlcoholMergePreviewResponse preview(AdminAlcoholMergeRequest request) {
    AdminAlcoholMergePreviewResponse response = execute(request);
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    return response;
  }

  @Transactional
  public AdminResultResponse merge(AdminAlcoholMergeRequest request) {
    execute(request);
    return AdminResultResponse.of(ALCOHOL_MERGED, request.targetAlcoholId());
  }

  @Transactional(readOnly = true)
  public GlobalResponse searchLogs(AdminAlcoholMergeLogSearchRequest request) {
    return GlobalResponse.fromPage(
        alcoholMergeRepository
            .searchLogs(
                request.targetAlcoholId(),
                request.sourceAlcoholId(),
                request.page(),
                request.size())
            .map(AdminAlcoholMergeService::toLogResponse));
  }

  private AdminAlcoholMergePreviewResponse execute(AdminAlcoholMergeRequest request) {
    Long targetId = request.targetAlcoholId();
    List<Long> sourceIds = request.sourceAlcoholIds();
    if (sourceIds.contains(targetId)) {
      throw new AlcoholException(ALCOHOL_MERGE_SELF);
    }

    List<Long> alcoholIds = new ArrayList<>(sourceIds);
    alcoholIds.add(targetId);
    Map<Long, Alcohol> alcohols =
        alcoholMergeRepository.lockAlcohols(alcoholIds).stream()
            .collect(Collectors.toMap(Alcohol::getId, Function.identity()));
    if (alcohols.size() != alcoholIds.size()) {
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    }
    Alcohol target = alcohols.get(targetId);
    if (target.isDeleted()) {
      throw new AlcoholException(ALCOHOL_ALREADY_DELETED);
    }

    String mergeGroupId = UUID.randomUUID().toString();
    List<SourceItem> sources = new ArrayList<>();
    for (Long sourceId : sourceIds) {
      Alcohol source = alcohols.get(sourceId);
      List<AlcoholMergeCount> counts =
          referenceMergers.stream()
              .flatMap(merger -> merger.merge(sourceId, targetId).stream())
              .toList();
      alcoholMergeRepository.save(AlcoholMergeLog.of(mergeGroupId, target, source, counts));
      alcoholMergeRepository.deleteAlcohol(sourceId);
      sources.add(new SourceItem(toAlcoholItem(source), counts));
    }
    return new AdminAlcoholMergePreviewResponse(toAlcoholItem(target), sources);
  }

  private static AlcoholItem toAlcoholItem(Alcohol alcohol) {
    return new AlcoholItem(
        alcohol.getId(), alcohol.getKorName(), alcohol.getEngName(), alcohol.getImageUrl());
  }

  private static AdminAlcoholMergeLogResponse toLogResponse(AlcoholMergeLog log) {
    return new AdminAlcoholMergeLogResponse(
        log.getId(),
        log.getMergeGroupId(),
        log.getTargetAlcoholId(),
        log.getTargetKorName(),
        log.getSourceAlcoholId(),
        log.getSourceKorName(),
        log.getSourceEngName(),
        log.getCounts(),
        log.getCreatePrincipal() != null ? log.getCreatePrincipal().getId() : null,
        log.getCreatePrincipal() != null ? log.getCreatePrincipal().getEmail() : null,
        log.getCreateAt());
  }
}
