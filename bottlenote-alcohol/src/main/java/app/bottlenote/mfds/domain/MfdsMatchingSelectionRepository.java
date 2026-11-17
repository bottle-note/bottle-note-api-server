package app.bottlenote.mfds.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.Collection;
import java.util.List;

@DomainRepository
public interface MfdsMatchingSelectionRepository {
  MfdsMatchingSelection save(MfdsMatchingSelection selection);

  /** 여러 신고의 선택 이력을 한 번에 최신순으로 조회한다. 일괄 매칭이 관리자 해제 여부를 판단할 때 쓴다. */
  List<MfdsMatchingSelection> findByDeclarationIdInOrderBySelectedAtDescIdDesc(
      Collection<Long> declarationIds);
}
