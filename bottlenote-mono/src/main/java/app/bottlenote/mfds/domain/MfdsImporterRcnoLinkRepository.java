package app.bottlenote.mfds.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;
import java.util.Optional;

/**
 * RCNO별 수입사 연결 근거(MfdsImporterRcnoLink) 저장·조회 포트. Spring/JPA 타입을 노출하지 않는다.
 *
 * <p>기본 키는 rcno(수입신고번호)다.
 */
@DomainRepository
public interface MfdsImporterRcnoLinkRepository {

  MfdsImporterRcnoLink save(MfdsImporterRcnoLink link);

  Optional<MfdsImporterRcnoLink> findByRcno(String rcno);

  List<MfdsImporterRcnoLink> findAllByImporterId(Long importerId);

  long countByImporterId(Long importerId);
}
