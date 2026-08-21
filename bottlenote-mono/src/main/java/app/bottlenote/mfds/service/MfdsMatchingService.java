package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_ALCOHOL_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_DISTILLERY_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_REGION_NOT_FOUND;

import app.bottlenote.alcohols.facade.AlcoholMatchTargetFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsMatchCandidate;
import app.bottlenote.mfds.dto.request.MfdsMatchingConfirmRequest;
import app.bottlenote.mfds.dto.response.MfdsAlcoholCandidateItem;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetailItem;
import app.bottlenote.mfds.dto.response.MfdsMatchingCandidatesResponse;
import app.bottlenote.mfds.dto.response.MfdsMatchingCandidatesResponse.MfdsMatchingSelection;
import app.bottlenote.mfds.dto.response.MfdsMatchingConfirmResponse;
import app.bottlenote.mfds.dto.response.MfdsMatchingRunResponse;
import app.bottlenote.mfds.dto.response.MfdsReferenceCandidateItem;
import app.bottlenote.mfds.exception.MfdsException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
  public static final String MATCHING_VERSION = "mfds-matching-v1";

  private static final BigDecimal CANDIDATE_SCORE_THRESHOLD = new BigDecimal("0.4");
  private static final int MAX_CANDIDATES = 3;

  private final MfdsDeclarationRepository declarationRepository;
  private final AlcoholMatchTargetFacade alcoholMatchTargetFacade;
  private final MfdsMatchingScoreCalculator scoreCalculator;

  /** 후보를 계산해 저장하고 점수 근거와 함께 반환한다. 기존 후보는 덮어쓴다. */
  @Transactional
  public MfdsMatchingRunResponse runMatching(Long declarationId) {
    MfdsDeclaration declaration = getDeclaration(declarationId);

    List<ScoredAlcohol> alcoholCandidates = rankAlcoholCandidates(declaration);
    List<ScoredReference> distilleryCandidates =
        rankReferenceCandidates(
            alcoholMatchTargetFacade.findAllDistilleryTargets().stream()
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
            alcoholMatchTargetFacade.findAllRegionTargets().stream()
                .map(
                    target ->
                        new ScoredReference(
                            target.id(),
                            target.korName(),
                            target.engName(),
                            scoreCalculator.scoreRegion(declaration, target)))
                .toList());

    LocalDateTime matchedAt = LocalDateTime.now();
    declaration.applyMatchingCandidates(
        alcoholCandidates.stream()
            .map(scored -> new MfdsMatchCandidate(scored.target().alcoholId(), scored.totalScore()))
            .toList(),
        distilleryCandidates.stream()
            .map(scored -> new MfdsMatchCandidate(scored.id(), scored.score()))
            .toList(),
        regionCandidates.stream()
            .map(scored -> new MfdsMatchCandidate(scored.id(), scored.score()))
            .toList(),
        MATCHING_VERSION,
        matchedAt);
    declarationRepository.save(declaration);

    return new MfdsMatchingRunResponse(
        declaration.getId(),
        MATCHING_VERSION,
        matchedAt,
        alcoholCandidates.stream()
            .map(scored -> toAlcoholItem(scored.target(), scored.totalScore(), scored.detail()))
            .toList(),
        distilleryCandidates.stream().map(MfdsMatchingService::toReferenceItem).toList(),
        regionCandidates.stream().map(MfdsMatchingService::toReferenceItem).toList());
  }

  /** 저장된 후보와 확정 상태를 각 후보의 요약 정보와 함께 조회한다. */
  @Transactional(readOnly = true)
  public MfdsMatchingCandidatesResponse getCandidates(Long declarationId) {
    MfdsDeclaration declaration = getDeclaration(declarationId);

    List<MfdsMatchCandidate> alcoholCandidates = declaration.getAlcoholCandidates();
    Map<Long, AlcoholMatchTargetItem> alcoholSummaries =
        alcoholMatchTargetFacade
            .findAlcoholTargetsByIds(
                alcoholCandidates.stream().map(MfdsMatchCandidate::id).toList())
            .stream()
            .collect(Collectors.toMap(AlcoholMatchTargetItem::alcoholId, Function.identity()));

    return new MfdsMatchingCandidatesResponse(
        declaration.getId(),
        declaration.getMatchingVersion(),
        declaration.getMatchedAt(),
        new MfdsMatchingSelection(
            declaration.getSelectedAlcoholId(),
            declaration.getAlcoholMatchDecision(),
            declaration.getSelectedDistilleryId(),
            declaration.getDistilleryMatchSource(),
            declaration.getSelectedRegionId(),
            declaration.getRegionMatchSource()),
        alcoholCandidates.stream()
            .map(
                candidate -> {
                  AlcoholMatchTargetItem summary = alcoholSummaries.get(candidate.id());
                  if (summary == null) {
                    return new MfdsAlcoholCandidateItem(
                        candidate.id(),
                        candidate.score(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
                  }
                  return toAlcoholItem(summary, candidate.score(), null);
                })
            .toList(),
        toStoredReferenceItems(
            declaration.getDistilleryCandidates(),
            alcoholMatchTargetFacade.findAllDistilleryTargets().stream()
                .collect(
                    Collectors.toMap(
                        target -> target.id(),
                        target -> new ReferenceName(target.korName(), target.engName())))),
        toStoredReferenceItems(
            declaration.getRegionCandidates(),
            alcoholMatchTargetFacade.findAllRegionTargets().stream()
                .collect(
                    Collectors.toMap(
                        target -> target.id(),
                        target -> new ReferenceName(target.korName(), target.engName())))));
  }

  /** 매칭을 확정한다. 후보 목록에 있으면 CANDIDATE, 후보 밖 ID면 MANUAL로 결정 근거를 기록한다. */
  @Transactional
  public MfdsMatchingConfirmResponse confirmMatching(
      Long declarationId, MfdsMatchingConfirmRequest request) {
    MfdsDeclaration declaration = getDeclaration(declarationId);

    if (!alcoholMatchTargetFacade.existsAlcohol(request.alcoholId())) {
      throw new MfdsException(MFDS_SELECTED_ALCOHOL_NOT_FOUND);
    }
    if (request.distilleryId() != null
        && !alcoholMatchTargetFacade.existsDistillery(request.distilleryId())) {
      throw new MfdsException(MFDS_SELECTED_DISTILLERY_NOT_FOUND);
    }
    if (request.regionId() != null && !alcoholMatchTargetFacade.existsRegion(request.regionId())) {
      throw new MfdsException(MFDS_SELECTED_REGION_NOT_FOUND);
    }

    declaration.confirmMatching(
        request.alcoholId(),
        selectionSource(declaration.hasAlcoholCandidate(request.alcoholId())),
        request.distilleryId(),
        request.distilleryId() != null
            ? selectionSource(declaration.hasDistilleryCandidate(request.distilleryId()))
            : null,
        request.regionId(),
        request.regionId() != null
            ? selectionSource(declaration.hasRegionCandidate(request.regionId()))
            : null);
    declarationRepository.save(declaration);

    return toConfirmResponse(declaration);
  }

  /** 확정을 해제한다. 저장된 후보와 매칭 이력은 유지한다. */
  @Transactional
  public MfdsMatchingConfirmResponse clearMatching(Long declarationId) {
    MfdsDeclaration declaration = getDeclaration(declarationId);
    declaration.clearMatchingSelection();
    declarationRepository.save(declaration);
    return toConfirmResponse(declaration);
  }

  private List<ScoredAlcohol> rankAlcoholCandidates(MfdsDeclaration declaration) {
    return alcoholMatchTargetFacade.findAllAlcoholTargets().stream()
        .map(target -> new ScoredAlcohol(target, scoreCalculator.scoreAlcohol(declaration, target)))
        .filter(scored -> scored.totalScore().compareTo(CANDIDATE_SCORE_THRESHOLD) >= 0)
        .sorted(
            Comparator.comparing(ScoredAlcohol::totalScore)
                .reversed()
                .thenComparing(scored -> scored.target().alcoholId()))
        .limit(MAX_CANDIDATES)
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
        .limit(MAX_CANDIDATES)
        .toList();
  }

  private MfdsDeclaration getDeclaration(Long declarationId) {
    return declarationRepository
        .findById(declarationId)
        .orElseThrow(() -> new MfdsException(MFDS_DECLARATION_NOT_FOUND));
  }

  private static MfdsMatchingConfirmResponse toConfirmResponse(MfdsDeclaration declaration) {
    return new MfdsMatchingConfirmResponse(
        declaration.getId(),
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholMatchDecision(),
        declaration.getSelectedDistilleryId(),
        declaration.getDistilleryMatchSource(),
        declaration.getSelectedRegionId(),
        declaration.getRegionMatchSource());
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

  private static List<MfdsReferenceCandidateItem> toStoredReferenceItems(
      List<MfdsMatchCandidate> candidates, Map<Long, ReferenceName> names) {
    return candidates.stream()
        .map(
            candidate -> {
              ReferenceName name = names.get(candidate.id());
              return new MfdsReferenceCandidateItem(
                  candidate.id(),
                  candidate.score(),
                  name != null ? name.korName() : null,
                  name != null ? name.engName() : null);
            })
        .toList();
  }

  private record ScoredAlcohol(AlcoholMatchTargetItem target, MfdsMatchScoreDetailItem detail) {
    BigDecimal totalScore() {
      return detail.totalScore();
    }
  }

  private record ScoredReference(Long id, String korName, String engName, BigDecimal score) {}

  private record ReferenceName(String korName, String engName) {}
}
