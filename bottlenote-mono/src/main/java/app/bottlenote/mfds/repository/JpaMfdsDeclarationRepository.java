package app.bottlenote.mfds.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
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

  /** 포트의 상한 파라미터를 Spring Data {@link Limit}으로 옮긴다. Spring Data 타입은 구현 안에만 둔다. */
  @Override
  default List<MfdsDeclaration> findNormalizedBySelectedAlcoholId(Long alcoholId, int limit) {
    return findBySelectedAlcoholIdAndNormalizationStatusOrderByIdDesc(
        alcoholId, MfdsNormalizationStatus.NORMALIZED, Limit.of(limit));
  }

  List<MfdsDeclaration> findBySelectedAlcoholIdAndNormalizationStatusOrderByIdDesc(
      Long alcoholId, MfdsNormalizationStatus normalizationStatus, Limit limit);

  /** SELECT ... FOR UPDATE로 행을 배타 잠금한 뒤 조회한다. 트랜잭션 경계 안에서만 유효하다. */
  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from mfds_declaration d where d.id = :id")
  Optional<MfdsDeclaration> findByIdForUpdate(@Param("id") Long id);

  @Override
  boolean existsByImporterId(Long importerId);
}
