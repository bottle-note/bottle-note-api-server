package app.bottlenote.mfds.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;

@DomainRepository
public interface MfdsMatchingSelectionRepository {
  MfdsMatchingSelection save(MfdsMatchingSelection selection);

  /** 신고의 선택 이력을 최신순으로 조회한다. 관리자 해제 여부를 다시 확인할 때 쓴다. */
  List<MfdsMatchingSelection> findByDeclarationIdOrderBySelectedAtDescIdDesc(Long declarationId);
}
