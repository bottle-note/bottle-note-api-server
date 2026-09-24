package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_ALCOHOL_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_DISTILLERY_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_REGION_NOT_FOUND;

import app.bottlenote.alcohols.facade.AlcoholMatchTargetFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsMatchingCandidate;
import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.domain.MfdsMatchingSelectionRepository;
import app.bottlenote.mfds.dto.request.MfdsMatchingConfirmRequest;
import app.bottlenote.mfds.dto.request.MfdsMatchingExecutionRequest;
import app.bottlenote.mfds.dto.request.MfdsMatchingReferenceSnapshotItem;
import app.bottlenote.mfds.dto.response.MfdsAlcoholCandidateItem;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetailItem;
import app.bottlenote.mfds.dto.response.MfdsMatchingCandidatesResponse;
import app.bottlenote.mfds.dto.response.MfdsMatchingConfirmResponse;
import app.bottlenote.mfds.dto.response.MfdsMatchingRunResponse;
import app.bottlenote.mfds.dto.response.MfdsReferenceCandidateItem;
import app.bottlenote.mfds.exception.MfdsException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 정제 수입 원장 한 건을 원본 알코올 데이터와 비교해 후보를 계산·저장하고 확정을 관리한다. */
@Service
@RequiredArgsConstructor
public class MfdsMatchingService {

  /** 점수 산식 버전. 산식(가중치·요소) 변경 시 올린다. */
  public static final String MATCHING_VERSION = "mfds-matching-v2";

  private static final BigDecimal CANDIDATE_SCORE_THRESHOLD = new BigDecimal("0.4");
  private static final int MAX_ALCOHOL_CANDIDATES = 10;
  private static final int MAX_REFERENCE_CANDIDATES = 3;

  private final MfdsDeclarationRepository declarationRepository;
  private final AlcoholMatchTargetFacade alcoholMatchTargetFacade;
  private final MfdsMatchingScoreCalculator scoreCalculator;
  private final MfdsMatchingSelectionRepository selectionRepository;
  private final MfdsMatchingHistoryService historyService;

  /** 후보를 계산해 저장하고 점수 근거와 함께 반환한다. 실행별 이력을 보존하고 최신 실행을 연결한다. */
  @Transactional
  public MfdsMatchingRunResponse runMatching(Long declarationId) {
    MfdsDeclaration declaration = getDeclarationForUpdate(declarationId);
    LocalDateTime startedAt = LocalDateTime.now();
    var alcoholTargets =
        alcoholMatchTargetFacade.findAllAlcoholTargets().stream()
            .sorted(Comparator.comparing(AlcoholMatchTargetItem::alcoholId))
            .toList();
    var distilleryTargets =
        alcoholMatchTargetFacade.findAllDistilleryTargets().stream()
            .sorted(Comparator.comparing(DistilleryMatchTargetItem::id))
            .toList();
    var regionTargets =
        alcoholMatchTargetFacade.findAllRegionTargets().stream()
            .sorted(Comparator.comparing(RegionMatchTargetItem::id))
            .toList();
    List<ScoredAlcohol> alcoholCandidates = rankAlcoholCandidates(declaration, alcoholTargets);
    List<ScoredReference> distilleryCandidates =
        rankReferenceCandidates(
            distilleryTargets.stream()
                .map(
                    target ->
                        new ScoredReference(
                            target.id(),
                            target.korName(),
                            target.engName(),
                            scoreCalculator.scoreDistillery(declaration, target)))
                .toList());
    List<ScoredReference> regionCandidates =
        rankReferenceCandidates(
            regionTargets.stream()
                .map(
                    target ->
                        new ScoredReference(
                            target.id(),
                            target.korName(),
                            target.engName(),
                            scoreCalculator.scoreRegion(declaration, target)))
                .toList());

    LocalDateTime matchedAt = LocalDateTime.now();
    var alcoholItems =
        alcoholCandidates.stream()
            .map(scored -> toAlcoholItem(scored.target(), scored.totalScore(), scored.detail()))
            .toList();
    var distilleryItems =
        distilleryCandidates.stream().map(MfdsMatchingService::toReferenceItem).toList();
    var regionItems = regionCandidates.stream().map(MfdsMatchingService::toReferenceItem).toList();
    Long runId =
        historyService.save(
            declaration,
            new MfdsMatchingExecutionRequest(
                MATCHING_VERSION,
                startedAt,
                matchedAt,
                new MfdsMatchingReferenceSnapshotItem(
                    alcoholTargets, distilleryTargets, regionTargets),
                alcoholItems,
                distilleryItems,
                regionItems));
    declaration.applyMatchingRun(runId, MATCHING_VERSION, matchedAt);
    declarationRepository.save(declaration);

    return new MfdsMatchingRunResponse(
        declaration.getId(),
        MATCHING_VERSION,
        matchedAt,
        alcoholItems,
        distilleryItems,
        regionItems);
  }

  /** 저장된 후보와 확정 상태를 각 후보의 요약 정보와 함께 조회한다. */
  @Transactional(readOnly = true)
  public MfdsMatchingCandidatesResponse getCandidates(Long declarationId) {
    MfdsDeclaration declaration = getDeclaration(declarationId);

    var candidates = historyService.findCandidates(declaration);
    var details = historyService.findDetails(candidates);
    var alcoholCandidates =
        candidates.stream().filter(c -> "ALCOHOL".equals(c.getTargetType())).toList();
    var summaries =
        alcoholMatchTargetFacade
            .findAlcoholTargetsByIds(
                alcoholCandidates.stream().map(MfdsMatchingCandidate::getTargetId).toList())
            .stream()
            .collect(Collectors.toMap(AlcoholMatchTargetItem::alcoholId, Function.identity()));
    return new MfdsMatchingCandidatesResponse(
        declaration.getId(),
        declaration.getMatchingVersion(),
        declaration.getMatchedAt(),
        new MfdsMatchingCandidatesResponse.MfdsMatchingSelection(
            declaration.getSelectedAlcoholId(),
            declaration.getAlcoholMatchDecision(),
            declaration.getSelectedDistilleryId(),
            declaration.getDistilleryMatchSource(),
            declaration.getSelectedRegionId(),
            declaration.getRegionMatchSource()),
        alcoholCandidates.stream()
            .map(
                candidate -> {
                  var summary = summaries.get(candidate.getTargetId());
                  if (summary == null)
                    return new MfdsAlcoholCandidateItem(
                        candidate.getTargetId(),
                        candidate.getRawScore(),
                        candidate.getTargetNameKo(),
                        candidate.getTargetNameEn(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        details.get(candidate.getId()));
                  return toAlcoholItem(
                      summary, candidate.getRawScore(), details.get(candidate.getId()));
                })
            .toList(),
        storedReferences(candidates, "DISTILLERY"),
        storedReferences(candidates, "REGION"));
  }

  private static List<MfdsReferenceCandidateItem> storedReferences(
      List<MfdsMatchingCandidate> candidates, String type) {
    return candidates.stream()
        .filter(c -> type.equals(c.getTargetType()))
        .map(
            c ->
                new MfdsReferenceCandidateItem(
                    c.getTargetId(), c.getRawScore(), c.getTargetNameKo(), c.getTargetNameEn()))
        .toList();
  }

  /** 매칭을 확정한다. 후보 목록에 있으면 CANDIDATE, 후보 밖 ID면 MANUAL로 결정 근거를 기록한다. */
  @Transactional
  public MfdsMatchingConfirmResponse confirmMatching(
      Long declarationId, MfdsMatchingConfirmRequest request, Long adminId) {
    MfdsDeclaration declaration = getDeclarationForUpdate(declarationId);
    MatchTarget target =
        resolveTarget(request.alcoholId(), request.distilleryId(), request.regionId());
    applyTarget(
        declaration,
        target,
        new SelectionAuditContext(declarationId, adminId, LocalDateTime.now(), null));
    return toConfirmResponse(declaration);
  }

  /** 주류·증류소·지역 존재를 확인하고, 생략한 증류소·지역은 주류에 등록된 값으로 채운다. */
  MatchTarget resolveTarget(Long alcoholId, Long distilleryId, Long regionId) {
    AlcoholMatchTargetItem alcohol =
        alcoholMatchTargetFacade.findAlcoholTargetsByIds(List.of(alcoholId)).stream()
            .findFirst()
            .orElseThrow(() -> new MfdsException(MFDS_SELECTED_ALCOHOL_NOT_FOUND));
    if (distilleryId != null && !alcoholMatchTargetFacade.existsDistillery(distilleryId)) {
      throw new MfdsException(MFDS_SELECTED_DISTILLERY_NOT_FOUND);
    }
    if (regionId != null && !alcoholMatchTargetFacade.existsRegion(regionId)) {
      throw new MfdsException(MFDS_SELECTED_REGION_NOT_FOUND);
    }
    return new MatchTarget(
        alcohol,
        distilleryId,
        regionId,
        distilleryId != null ? distilleryId : positiveId(alcohol.distilleryId()),
        regionId != null ? regionId : positiveId(alcohol.regionId()));
  }

  /** 대상을 신고에 반영하고 선택 근거를 감사 이력으로 남긴다. 단건과 일괄 확정이 함께 쓴다. */
  void applyTarget(MfdsDeclaration declaration, MatchTarget target, SelectionAuditContext audit) {
    AlcoholMatchTargetItem alcohol = target.alcohol();
    var candidates = historyService.findCandidates(declaration);
    Long previousDistilleryId = declaration.getSelectedDistilleryId();
    Long previousRegionId = declaration.getSelectedRegionId();
    Long distilleryId = target.distilleryId();
    Long regionId = target.regionId();
    declaration.confirmMatching(
        alcohol.alcoholId(),
        selectionSource(hasCandidate(candidates, "ALCOHOL", alcohol.alcoholId())),
        distilleryId,
        referenceSource(
            target.requestedDistilleryId(),
            distilleryId,
            hasCandidate(candidates, "DISTILLERY", distilleryId)),
        regionId,
        referenceSource(
            target.requestedRegionId(), regionId, hasCandidate(candidates, "REGION", regionId)));
    declaration.applyMatchedAlcoholName(alcohol.korName(), alcohol.engName());
    declarationRepository.save(declaration);

    recordSelection(
        audit,
        "ALCOHOL",
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholMatchDecision());
    recordReferenceSelection(
        audit,
        "DISTILLERY",
        previousDistilleryId,
        distilleryId,
        declaration.getDistilleryMatchSource());
    recordReferenceSelection(
        audit, "REGION", previousRegionId, regionId, declaration.getRegionMatchSource());
  }

  /** 확정을 해제한다. 저장된 후보와 매칭 이력은 유지한다. */
  @Transactional
  public MfdsMatchingConfirmResponse clearMatching(Long declarationId, Long adminId) {
    MfdsDeclaration declaration = getDeclarationForUpdate(declarationId);
    SelectionAuditContext audit =
        new SelectionAuditContext(declarationId, adminId, LocalDateTime.now(), null);
    recordRevocation(audit, "ALCOHOL", declaration.getSelectedAlcoholId(), "ADMIN_RELEASE");
    recordRevocation(audit, "DISTILLERY", declaration.getSelectedDistilleryId(), "ADMIN_RELEASE");
    recordRevocation(audit, "REGION", declaration.getSelectedRegionId(), "ADMIN_RELEASE");
    declaration.clearMatchingSelection();
    declarationRepository.save(declaration);
    return toConfirmResponse(declaration);
  }

  private static Long positiveId(Long id) {
    return id != null && id > 0 ? id : null;
  }

  private static MfdsMatchSelectionSource referenceSource(
      Long requestedId, Long selectedId, boolean fromCandidate) {
    if (selectedId == null) {
      return null;
    }
    return requestedId != null
        ? selectionSource(fromCandidate)
        : MfdsMatchSelectionSource.ALCOHOL_PROPAGATED;
  }

  private void recordReferenceSelection(
      SelectionAuditContext audit,
      String targetType,
      Long previousId,
      Long selectedId,
      String reasonCode) {
    if (selectedId != null) {
      recordSelection(audit, targetType, selectedId, reasonCode);
    } else {
      recordRevocation(audit, targetType, previousId, "ADMIN_SELECTION_CLEARED");
    }
  }

  private void recordSelection(
      SelectionAuditContext audit, String targetType, Long targetId, String reasonCode) {
    selectionRepository.save(
        MfdsMatchingSelection.adminSelect(
            audit.declarationId(),
            targetType,
            targetId,
            audit.reasonCode(reasonCode),
            audit.adminId(),
            audit.selectedAt()));
  }

  private void recordRevocation(
      SelectionAuditContext audit, String targetType, Long targetId, String reasonCode) {
    if (targetId != null) {
      selectionRepository.save(
          MfdsMatchingSelection.adminRevoke(
              audit.declarationId(),
              targetType,
              targetId,
              reasonCode,
              audit.adminId(),
              audit.selectedAt()));
    }
  }

  private List<ScoredAlcohol> rankAlcoholCandidates(
      MfdsDeclaration declaration, List<AlcoholMatchTargetItem> targets) {
    return targets.stream()
        .map(target -> new ScoredAlcohol(target, scoreCalculator.scoreAlcohol(declaration, target)))
        .filter(scored -> scored.totalScore().compareTo(CANDIDATE_SCORE_THRESHOLD) >= 0)
        .sorted(
            Comparator.comparing(ScoredAlcohol::totalScore)
                .reversed()
                .thenComparing(scored -> scored.target().alcoholId()))
        .limit(MAX_ALCOHOL_CANDIDATES)
        .toList();
  }

  private List<ScoredReference> rankReferenceCandidates(List<ScoredReference> scoredTargets) {
    return scoredTargets.stream()
        .filter(
            scored ->
                scored.score() != null && scored.score().compareTo(CANDIDATE_SCORE_THRESHOLD) >= 0)
        .sorted(
            Comparator.comparing(ScoredReference::score)
                .reversed()
                .thenComparing(ScoredReference::id))
        .limit(MAX_REFERENCE_CANDIDATES)
        .toList();
  }

  private MfdsDeclaration getDeclaration(Long declarationId) {
    return declarationRepository
        .findById(declarationId)
        .orElseThrow(() -> new MfdsException(MFDS_DECLARATION_NOT_FOUND));
  }

  /** 확정·해제처럼 읽은 뒤 갱신하는 경로에서 쓴다. 행을 배타 잠금해 동시 요청의 마지막 쓰기 승리를 막는다. */
  private MfdsDeclaration getDeclarationForUpdate(Long declarationId) {
    return declarationRepository
        .findByIdForUpdate(declarationId)
        .orElseThrow(() -> new MfdsException(MFDS_DECLARATION_NOT_FOUND));
  }

  static MfdsMatchingConfirmResponse toConfirmResponse(MfdsDeclaration declaration) {
    return new MfdsMatchingConfirmResponse(
        declaration.getId(),
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholMatchDecision(),
        declaration.getSelectedDistilleryId(),
        declaration.getDistilleryMatchSource(),
        declaration.getSelectedRegionId(),
        declaration.getRegionMatchSource());
  }

  private static boolean hasCandidate(
      List<MfdsMatchingCandidate> candidates, String type, Long id) {
    return candidates.stream()
        .anyMatch(c -> type.equals(c.getTargetType()) && c.getTargetId().equals(id));
  }

  private static MfdsMatchSelectionSource selectionSource(boolean fromCandidate) {
    return fromCandidate ? MfdsMatchSelectionSource.CANDIDATE : MfdsMatchSelectionSource.MANUAL;
  }

  private static MfdsAlcoholCandidateItem toAlcoholItem(
      AlcoholMatchTargetItem target, BigDecimal score, MfdsMatchScoreDetailItem detail) {
    return new MfdsAlcoholCandidateItem(
        target.alcoholId(),
        score,
        target.korName(),
        target.engName(),
        target.korCategory(),
        target.engCategory(),
        target.abv(),
        target.age(),
        target.imageUrl(),
        detail);
  }

  private static MfdsReferenceCandidateItem toReferenceItem(ScoredReference scored) {
    return new MfdsReferenceCandidateItem(
        scored.id(), scored.score(), scored.korName(), scored.engName());
  }

  /** 확정할 주류와 요청값·반영값. 요청값이 없으면 증류소·지역 근거는 주류 전파다. */
  record MatchTarget(
      AlcoholMatchTargetItem alcohol,
      Long requestedDistilleryId,
      Long requestedRegionId,
      Long distilleryId,
      Long regionId) {}

  /** 감사 이력 작성 방식. 일괄 확정은 근거 코드에 기준 신고를 붙인다. */
  record SelectionAuditContext(
      Long declarationId, Long adminId, LocalDateTime selectedAt, String reasonFormat) {
    String reasonCode(String source) {
      return reasonFormat == null ? source : reasonFormat.formatted(source);
    }
  }

  private record ScoredAlcohol(AlcoholMatchTargetItem target, MfdsMatchScoreDetailItem detail) {
    BigDecimal totalScore() {
      return detail.totalScore();
    }
  }

  private record ScoredReference(Long id, String korName, String engName, BigDecimal score) {}
}
