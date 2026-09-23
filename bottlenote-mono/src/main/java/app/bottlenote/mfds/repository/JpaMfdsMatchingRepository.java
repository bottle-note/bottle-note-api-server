package app.bottlenote.mfds.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.mfds.domain.MfdsMatchingCandidate;
import app.bottlenote.mfds.domain.MfdsMatchingEvidence;
import app.bottlenote.mfds.domain.MfdsMatchingRepository;
import app.bottlenote.mfds.domain.MfdsMatchingRun;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@JpaRepositoryImpl
@RequiredArgsConstructor
public class JpaMfdsMatchingRepository implements MfdsMatchingRepository {
  private final EntityManager entityManager;

  @Override
  public MfdsMatchingRun saveRun(MfdsMatchingRun run) {
    entityManager.persist(run);
    return run;
  }

  @Override
  public MfdsMatchingCandidate saveCandidate(MfdsMatchingCandidate candidate) {
    entityManager.persist(candidate);
    return candidate;
  }

  @Override
  public void saveEvidence(MfdsMatchingEvidence evidence) {
    entityManager.persist(evidence);
  }

  @Override
  public Optional<MfdsMatchingRun> findRun(Long runId) {
    return runId == null
        ? Optional.empty()
        : Optional.ofNullable(entityManager.find(MfdsMatchingRun.class, runId));
  }

  @Override
  public List<MfdsMatchingCandidate> findCandidates(Long runId, Long declarationId) {
    if (runId == null) return List.of();
    return entityManager
        .createQuery(
            "select c from MfdsMatchingCandidate c where c.runId = :runId and c.declarationId = :declarationId order by c.targetType, c.rankNo, c.id",
            MfdsMatchingCandidate.class)
        .setParameter("runId", runId)
        .setParameter("declarationId", declarationId)
        .getResultList();
  }

  @Override
  public List<MfdsMatchingEvidence> findEvidence(List<Long> candidateIds) {
    if (candidateIds.isEmpty()) return List.of();
    return entityManager
        .createQuery(
            "select e from MfdsMatchingEvidence e where e.candidateId in :ids order by e.id",
            MfdsMatchingEvidence.class)
        .setParameter("ids", candidateIds)
        .getResultList();
  }
}
