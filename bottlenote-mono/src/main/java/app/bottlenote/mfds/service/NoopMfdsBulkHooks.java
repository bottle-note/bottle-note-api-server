package app.bottlenote.mfds.service;

import org.springframework.stereotype.Component;

@Component
class NoopMfdsBulkLockGate implements MfdsBulkLockGate {

  @Override
  public void beforeGroupLock(Long sourceDeclarationId) {}
}

@Component
class NoopMfdsBulkSaveGuard implements MfdsBulkSaveGuard {

  @Override
  public void beforeSave(int index, Long declarationId) {}
}
