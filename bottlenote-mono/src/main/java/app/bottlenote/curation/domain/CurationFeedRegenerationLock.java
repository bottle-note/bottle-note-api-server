package app.bottlenote.curation.domain;

// 인스턴스가 여럿이면 기동 시 재생성이 중복 실행된다. JVM 로컬 상태로는 막을 수 없어 공유 저장소로 잠근다.
public interface CurationFeedRegenerationLock {

  boolean tryAcquire();

  void release();
}
