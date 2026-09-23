package app.bottlenote.mfds.domain;

import app.bottlenote.common.annotation.DomainRepository;

@DomainRepository
public interface MfdsMatchingSelectionRepository {
  MfdsMatchingSelection save(MfdsMatchingSelection selection);
}
