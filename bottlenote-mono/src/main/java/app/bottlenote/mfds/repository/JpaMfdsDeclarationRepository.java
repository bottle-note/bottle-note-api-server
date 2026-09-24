package app.bottlenote.mfds.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaMfdsDeclarationRepository
    extends MfdsDeclarationRepository,
        JpaRepository<MfdsDeclaration, Long>,
        CustomMfdsDeclarationRepository {

  @Override
  Optional<MfdsDeclaration> findByRcno(String rcno);

  /** SELECT ... FOR UPDATE로 행을 배타 잠금한 뒤 조회한다. 트랜잭션 경계 안에서만 유효하다. */
  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from mfds_declaration d where d.id = :id")
  Optional<MfdsDeclaration> findByIdForUpdate(@Param("id") Long id);

  @Override
  @Query("select d.id from mfds_declaration d where d.id = :id")
  Long findDeclarationId(@Param("id") Long id);

  @Override
  @Query("select d.productIdentityKeySha256 from mfds_declaration d where d.id = :id")
  byte[] findProductIdentityKeySha256ById(@Param("id") Long id);

  @Override
  @Query(
      "select d from mfds_declaration d where d.productIdentityKeySha256 = :key order by d.id asc")
  List<MfdsDeclaration> findByProductIdentityKeySha256(@Param("key") byte[] key);

  /** id 오름차순으로 잠가야 서로 다른 일괄 확정이 같은 그룹에서 교차 대기하지 않는다. */
  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select d from mfds_declaration d where d.productIdentityKeySha256 = :key order by d.id asc")
  List<MfdsDeclaration> findByProductIdentityKeySha256ForUpdate(@Param("key") byte[] key);

  @Override
  boolean existsByImporterId(Long importerId);
}
