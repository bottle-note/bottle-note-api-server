package app.bottlenote.mfds.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import app.bottlenote.mfds.dto.dsl.MfdsPublicAlcoholSearchCriteria;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholCategoryItem;
import app.bottlenote.mfds.dto.response.MfdsPublicCountryItem;
import java.util.Collection;
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

  /** 엔티티를 영속성 컨텍스트에 올리지 않고 신고 존재만 확인한다. */
  Long findDeclarationId(Long id);

  /** 엔티티를 영속성 컨텍스트에 올리지 않고 제품 동일성 키만 읽는다. 행이 없으면 null이다. */
  byte[] findProductIdentityKeySha256ById(Long id);

  /**
   * 갱신을 전제로 신고 데이터를 조회한다. 조회한 행을 트랜잭션이 끝날 때까지 배타 잠금해 동시 확정·해제의 마지막 쓰기 승리를 막는다.
   *
   * <p>반드시 트랜잭션 경계 안에서 호출해야 하며, 조회 전용 경로에서는 {@link #findById(Long)}를 쓴다.
   */
  Optional<MfdsDeclaration> findByIdForUpdate(Long id);

  /** 같은 제품 동일성 키를 가진 신고를 id 오름차순으로 조회한다. 키가 없으면 빈 목록이다. */
  List<MfdsDeclaration> findByProductIdentityKeySha256(byte[] productIdentityKeySha256);

  /**
   * 주어진 신고를 id 오름차순으로 잠그고 조회한다. 일괄 확정은 이 순서로만 잠가 교착을 피한다. 없는 id는 결과에서 빠진다.
   *
   * <p>반드시 트랜잭션 경계 안에서 호출해야 한다.
   */
  List<MfdsDeclaration> findByIdInForUpdate(Collection<Long> ids);

  Optional<MfdsDeclaration> findByRcno(String rcno);

  Optional<MfdsItem> findLatestItemByRcno(String rcno);

  /** 검색 조건에 맞는 신고 데이터를 id 내림차순으로 조회한다. limit은 pageSize+1(hasNext 판별)을 포함한다. */
  List<MfdsDeclaration> searchByCriteria(MfdsDeclarationSearchCriteria criteria);

  /** cursor를 제외한 목록 조회 조건의 전체 건수를 반환한다. */
  long countByCriteria(MfdsDeclarationSearchCriteria criteria);

  /** 해당 수입사에 연결된 신고 데이터 존재 여부를 확인한다. 수입사 삭제 가드에 쓴다. */
  boolean existsByImporterId(Long importerId);

  /** Product 공개 수입 주류 목록. 정규화 완료 행만 내리며 limit은 pageSize+1이다. */
  List<MfdsDeclaration> searchPublicAlcohols(MfdsPublicAlcoholSearchCriteria criteria);

  /** 정규화 완료 원장에 등장한 수출국 ISO Alpha-2 목록. */
  List<MfdsPublicCountryItem> findExportCountries();

  /** 정규화 완료 원장의 카테고리 ko/en 중복 제거 목록과 공개 건수. */
  List<MfdsPublicAlcoholCategoryItem> findAlcoholCategories();
}
