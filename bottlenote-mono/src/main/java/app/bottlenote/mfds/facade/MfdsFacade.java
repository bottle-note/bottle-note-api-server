package app.bottlenote.mfds.facade;

import app.bottlenote.mfds.facade.payload.MfdsPublicDeclarationItem;
import java.util.List;

/** 타 도메인에 공개하는 MFDS 조회 계약. */
public interface MfdsFacade {

  /** 한 주류에 공개하는 신고 최대 건수. 신고가 누적되어도 응답 크기가 늘지 않도록 최신(id 내림차순) 기준으로 자른다. */
  int MAX_PUBLIC_DECLARATIONS = 20;

  /**
   * 주류에 검증(매칭 확정·정규화 완료)된 수입 신고 공개 정보를 최신 순 최대 {@value #MAX_PUBLIC_DECLARATIONS}건 반환한다.
   *
   * <p>조건은 selectedAlcoholId 일치 AND normalizationStatus=NORMALIZED다. 연결 없음·상태 미완료(검토중 등)는 빈 목록이다.
   * 내부 점수·사유·검토 메모·원문은 포함하지 않는다.
   *
   * <p>수입사는 ACTIVE만 노출한다. 미연결이거나 INACTIVE 수입사에 연결된 신고는 importer가 null이다.
   */
  List<MfdsPublicDeclarationItem> findVerifiedDeclarationsByAlcoholId(Long alcoholId);
}
