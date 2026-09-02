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

  @Override
  @Query(
      "select d from mfds_declaration d where d.selectedAlcoholId = :alcoholId order by d.id desc")
  List<MfdsDeclaration> findAllBySelectedAlcoholId(@Param("alcoholId") Long alcoholId);

  /** SELECT ... FOR UPDATE로 행을 배타 잠금한 뒤 조회한다. 트랜잭션 경계 안에서만 유효하다. */
  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from mfds_declaration d where d.id = :id")
  Optional<MfdsDeclaration> findByIdForUpdate(@Param("id") Long id);

  @Override
  boolean existsByImporterId(Long importerId);
}
