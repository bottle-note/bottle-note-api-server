package app.bottlenote.mfds.service;

import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsMatchingCandidate;
import app.bottlenote.mfds.domain.MfdsMatchingEvidence;
import app.bottlenote.mfds.domain.MfdsMatchingRepository;
import app.bottlenote.mfds.domain.MfdsMatchingRun;
import app.bottlenote.mfds.dto.request.MfdsMatchingExecutionRequest;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetailItem;
import app.bottlenote.mfds.dto.response.MfdsReferenceCandidateItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 후보와 계산 근거는 실행별로 보존하며 신고의 과거 후보 컬럼으로 되돌아가지 않는다. */
@Service
@RequiredArgsConstructor
public class MfdsMatchingHistoryService {
  private static final String DETAIL_FEATURE = "ADMIN_SCORE_DETAIL_V2";
  private final MfdsMatchingRepository repository;
  private final MfdsMatchingEvidenceCodec codec;

  @Transactional
  public Long save(MfdsDeclaration declaration, MfdsMatchingExecutionRequest execution) {
    var version = execution.version();
    var startedAt = execution.startedAt();
    var matchedAt = execution.matchedAt();
    var alcohols = execution.alcohols();
    var distilleries = execution.distilleries();
    var regions = execution.regions();
    var run =
        repository.saveRun(
            MfdsMatchingRun.builder()
                .matcherVersion(version)
                .referenceHash(codec.referenceHash(execution.references()))
                .normalizationVersion(declaration.getNormalizationVersion())
                .scope("MATCH")
                .status("DONE")
                .weightsJson(
                    "{\"brand\":0.4,\"product\":0.35,\"age\":0.15,\"abv\":0.07,\"category\":0.03,\"scoreScale\":4,\"threshold\":0.4,\"alcoholLimit\":10,\"referenceLimit\":3}")
                .statsJson(codec.encodeStats(alcohols.size(), distilleries.size(), regions.size()))
                .startedAt(startedAt)
                .finishedAt(matchedAt)
                .createdAt(matchedAt)
                .build());
    for (int i = 0; i < alcohols.size(); i++) {
      var item = alcohols.get(i);
      var candidate =
          saveCandidate(
              new CandidateContext(run.getId(), declaration.getId(), matchedAt),
              "ALCOHOL",
              i + 1,
              new MfdsReferenceCandidateItem(
                  item.alcoholId(), item.score(), item.korName(), item.engName()));
      if (item.scoreDetail() != null) {
        repository.saveEvidence(
            MfdsMatchingEvidence.builder()
                .candidateId(candidate.getId())
                .featureCode(DETAIL_FEATURE)
                .evidenceSource("ADMIN_API")
                .inputValue(codec.encodeDetail(item.scoreDetail()))
                .ruleCode(version)
                .weight(BigDecimal.ZERO)
                .createdAt(matchedAt)
                .build());
        for (var comparison : item.scoreDetail().comparisons()) {
          repository.saveEvidence(
              MfdsMatchingEvidence.builder()
                  .candidateId(candidate.getId())
                  .featureCode(comparison.attribute())
                  .evidenceSource("ADMIN_API")
                  .inputValue(comparison.sourceValue())
                  .referenceValue(comparison.targetValue())
                  .ruleCode(comparison.status())
                  .weight(BigDecimal.ZERO)
                  .createdAt(matchedAt)
                  .build());
        }
      }
    }
    saveReferences(run.getId(), declaration.getId(), "DISTILLERY", distilleries, matchedAt);
    saveReferences(run.getId(), declaration.getId(), "REGION", regions, matchedAt);
    return run.getId();
  }

  @Transactional(readOnly = true)
  public List<MfdsMatchingCandidate> findCandidates(MfdsDeclaration declaration) {
    return repository
        .findRun(declaration.getMatchingRunId())
        .filter(run -> "DONE".equals(run.getStatus()))
        .filter(run -> Objects.equals(run.getMatcherVersion(), declaration.getMatchingVersion()))
        .map(run -> repository.findCandidates(run.getId(), declaration.getId()))
        .orElseGet(List::of);
  }

  @Transactional(readOnly = true)
  public Map<Long, MfdsMatchScoreDetailItem> findDetails(List<MfdsMatchingCandidate> candidates) {
    Map<Long, MfdsMatchScoreDetailItem> result = new LinkedHashMap<>();
    for (var evidence :
        repository.findEvidence(candidates.stream().map(MfdsMatchingCandidate::getId).toList())) {
      if (DETAIL_FEATURE.equals(evidence.getFeatureCode())) {
        result.put(evidence.getCandidateId(), codec.decodeDetail(evidence.getInputValue()));
      }
    }
    return result;
  }

  private void saveReferences(
      Long runId,
      Long declarationId,
      String type,
      List<MfdsReferenceCandidateItem> items,
      LocalDateTime now) {
    for (int i = 0; i < items.size(); i++) {
      var item = items.get(i);
      saveCandidate(new CandidateContext(runId, declarationId, now), type, i + 1, item);
    }
  }

  private MfdsMatchingCandidate saveCandidate(
      CandidateContext context, String type, int rank, MfdsReferenceCandidateItem item) {
    return repository.saveCandidate(
        MfdsMatchingCandidate.builder()
            .runId(context.runId())
            .declarationId(context.declarationId())
            .stage("ALCOHOL".equals(type) ? "A" : "B")
            .targetType(type)
            .targetId(item.id())
            .rankNo(rank)
            .rawScore(item.score())
            .evidenceStrength(0)
            .targetNameKo(item.korName())
            .targetNameEn(item.engName())
            .createdAt(context.now())
            .build());
  }

  private record CandidateContext(Long runId, Long declarationId, LocalDateTime now) {}
}
