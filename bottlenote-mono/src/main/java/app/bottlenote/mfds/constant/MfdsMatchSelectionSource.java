package app.bottlenote.mfds.constant;

/** 매칭 확정 시 선택 근거. 후보 목록에서 골랐는지, 후보 밖 수동 지정인지 구분한다. */
public enum MfdsMatchSelectionSource {
  CANDIDATE,
  MANUAL
}
