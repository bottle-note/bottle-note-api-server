package app.bottlenote.mfds.facade;

import app.bottlenote.mfds.facade.payload.MfdsPublicDeclarationItem;
import java.util.List;

/** 타 도메인에 공개하는 MFDS 조회 계약. */
public interface MfdsFacade {

  /**
   * 주류에 검증(매칭 확정·정규화 완료)된 수입 신고 공개 정보를 반환한다.
   *
   * <p>조건은 selectedAlcoholId 일치 AND normalizationStatus=NORMALIZED다. 연결 없음·상태 미완료(검토중 등)는
   * 빈 목록이다. 내부 점수·사유·검토 메모·원문은 포함하지 않는다.
   */
  List<MfdsPublicDeclarationItem> findVerifiedDeclarationsByAlcoholId(Long alcoholId);
}
