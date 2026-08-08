package app.bottlenote.accesscontrol.constant;

/** DB 현재 상태가 Redis enforcement에 반영됐는지 나타낸다. */
public enum ProjectionStatus {
  APPLIED,
  PENDING_RECONCILE
}
