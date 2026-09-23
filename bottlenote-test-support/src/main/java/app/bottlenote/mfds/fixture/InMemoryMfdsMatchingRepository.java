package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.domain.MfdsMatchingCandidate;
import app.bottlenote.mfds.domain.MfdsMatchingEvidence;
import app.bottlenote.mfds.domain.MfdsMatchingRepository;
import app.bottlenote.mfds.domain.MfdsMatchingRun;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryMfdsMatchingRepository implements MfdsMatchingRepository {
  private final AtomicLong ids = new AtomicLong();
  private final Map<Long, MfdsMatchingRun> runs = new LinkedHashMap<>();
  private final Map<Long, MfdsMatchingCandidate> candidates = new LinkedHashMap<>();
  private final List<MfdsMatchingEvidence> evidence = new ArrayList<>();

  @Override
  public MfdsMatchingRun saveRun(MfdsMatchingRun run) {
    ReflectionTestUtils.setField(run, "id", ids.incrementAndGet());
    runs.put(run.getId(), run); return run;
  }
  @Override
  public MfdsMatchingCandidate saveCandidate(MfdsMatchingCandidate candidate) {
    ReflectionTestUtils.setField(candidate, "id", ids.incrementAndGet());
    candidates.put(candidate.getId(), candidate); return candidate;
  }
  @Override
  public void saveEvidence(MfdsMatchingEvidence item) { evidence.add(item); }
  @Override
  public Optional<MfdsMatchingRun> findRun(Long id) { return Optional.ofNullable(runs.get(id)); }
  @Override
  public List<MfdsMatchingCandidate> findCandidates(Long runId, Long declarationId) {
    return candidates.values().stream().filter(c -> Objects.equals(runId, c.getRunId()) && Objects.equals(declarationId, c.getDeclarationId()))
        .sorted(Comparator.comparing(MfdsMatchingCandidate::getTargetType).thenComparing(MfdsMatchingCandidate::getRankNo)).toList();
  }
  @Override
  public List<MfdsMatchingEvidence> findEvidence(List<Long> candidateIds) {
    return evidence.stream().filter(e -> candidateIds.contains(e.getCandidateId())).toList();
  }
}
