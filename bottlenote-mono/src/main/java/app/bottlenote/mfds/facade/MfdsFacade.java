package app.bottlenote.mfds.facade;

import app.bottlenote.mfds.dto.response.MfdsPublicDeclarationItem;
import java.util.List;

/** 타 도메인에 공개하는 MFDS 조회 계약. */
public interface MfdsFacade {

  /**
   * 주류에 검증(매칭 확정)된 수입 신고 공개 정보를 반환한다.
   *
   * <p>연결 없음·미확정(검토중)은 빈 목록이다. 내부 점수·사유·검토 메모·원문은 포함하지 않는다.
   */
  List<MfdsPublicDeclarationItem> findVerifiedDeclarationsByAlcoholId(Long alcoholId);
}
