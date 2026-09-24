package app.bottlenote.mfds.service;

/** 그룹 잠금 직전에 호출한다. 운영에서는 비어 있고, 경합 테스트가 순서를 고정할 때만 대기한다. */
public interface MfdsBulkLockGate {

  void beforeGroupLock(Long sourceDeclarationId);
}
