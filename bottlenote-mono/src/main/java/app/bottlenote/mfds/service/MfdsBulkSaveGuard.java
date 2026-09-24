package app.bottlenote.mfds.service;

/** 확정 저장 직전 훅. 운영에서는 비어 있고, 롤백 테스트가 N번째 저장에서 예외를 던진다. */
public interface MfdsBulkSaveGuard {

  void beforeSave(int index, Long declarationId);
}
