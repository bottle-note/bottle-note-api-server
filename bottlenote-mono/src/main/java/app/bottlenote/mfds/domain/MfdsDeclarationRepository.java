package app.bottlenote.mfds.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import java.util.List;
import java.util.Optional;

/**
 * 신고 정제 데이터(MfdsDeclaration) 저장·조회 포트. Spring/JPA 타입을 노출하지 않는다.
 *
 * <p>원본 적재는 외부 수집기가 담당하며, 이 포트는 Admin 조회·검토 상태 갱신에 쓴다.
 */
@DomainRepository
public interface MfdsDeclarationRepository {

  MfdsDeclaration save(MfdsDeclaration declaration);

  Optional<MfdsDeclaration> findById(Long id);

  Optional<MfdsDeclaration> findByRcno(String rcno);

  /** 검색 조건에 맞는 신고 데이터를 id 내림차순으로 조회한다. limit은 pageSize+1(hasNext 판별)을 포함한다. */
  List<MfdsDeclaration> searchByCriteria(MfdsDeclarationSearchCriteria criteria);

  /** cursor를 제외한 목록 조회 조건의 전체 건수를 반환한다. */
  long countByCriteria(MfdsDeclarationSearchCriteria criteria);

  /** 해당 수입사에 연결된 신고 데이터 존재 여부를 확인한다. 수입사 삭제 가드에 쓴다. */
  boolean existsByImporterId(Long importerId);
}
