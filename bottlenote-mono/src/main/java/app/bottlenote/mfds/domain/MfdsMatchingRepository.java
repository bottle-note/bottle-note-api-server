package app.bottlenote.mfds.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface MfdsMatchingRepository {
  MfdsMatchingRun saveRun(MfdsMatchingRun run);

  MfdsMatchingCandidate saveCandidate(MfdsMatchingCandidate candidate);

  void saveEvidence(MfdsMatchingEvidence evidence);

  Optional<MfdsMatchingRun> findRun(Long runId);

  List<MfdsMatchingCandidate> findCandidates(Long runId, Long declarationId);

  List<MfdsMatchingEvidence> findEvidence(List<Long> candidateIds);
}
