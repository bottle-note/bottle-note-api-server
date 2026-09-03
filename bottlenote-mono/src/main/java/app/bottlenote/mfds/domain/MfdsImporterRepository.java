package app.bottlenote.mfds.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.dto.dsl.MfdsImporterSearchCriteria;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 식약처 공식 수입사(MfdsImporter) 저장·조회 포트. Spring/JPA 타입을 노출하지 않는다.
 *
 * <p>원본 적재는 외부 수집기가 담당하며, 이 포트는 Admin 조회·관리 상태 갱신에 쓴다.
 */
@DomainRepository
public interface MfdsImporterRepository {

  MfdsImporter save(MfdsImporter importer);

  void delete(MfdsImporter importer);

  Optional<MfdsImporter> findById(Long id);

  /**
   * 식별자 목록과 관리 상태로 수입사를 일괄 조회한다. 빈 목록이면 빈 결과를 반환한다.
   *
   * <p>공개 노출 경로는 ACTIVE만 조회해 INACTIVE 수입사의 대표자명·주소·전화번호가 새지 않게 한다.
   */
  List<MfdsImporter> findAllByIdInAndAdminStatus(
      Collection<Long> ids, MfdsImporterAdminStatus adminStatus);

  Optional<MfdsImporter> findByOfficialBusinessCode(String officialBusinessCode);

  /** 검색 조건에 맞는 수입사를 id 내림차순으로 조회한다. limit은 pageSize+1(hasNext 판별)을 포함한다. */
  List<MfdsImporter> searchByCriteria(MfdsImporterSearchCriteria criteria);

  /** cursor를 제외한 목록 조회 조건의 전체 건수를 반환한다. */
  long countByCriteria(MfdsImporterSearchCriteria criteria);
}
