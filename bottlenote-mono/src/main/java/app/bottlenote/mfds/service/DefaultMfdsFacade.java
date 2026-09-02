package app.bottlenote.mfds.service;

import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import app.bottlenote.mfds.dto.response.MfdsPublicDeclarationItem;
import app.bottlenote.mfds.dto.response.MfdsPublicImporterItem;
import app.bottlenote.mfds.facade.MfdsFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Product 등 타 도메인에 검증 완료 MFDS 공개 정보를 제공한다. */
@FacadeService
@RequiredArgsConstructor
public class DefaultMfdsFacade implements MfdsFacade {

  private final MfdsDeclarationRepository declarationRepository;
  private final MfdsImporterRepository importerRepository;

  @Override
  @Transactional(readOnly = true)
  public List<MfdsPublicDeclarationItem> findVerifiedDeclarationsByAlcoholId(Long alcoholId) {
    if (alcoholId == null) {
      return List.of();
    }
    return declarationRepository.findAllBySelectedAlcoholId(alcoholId).stream()
        .map(this::toPublicItem)
        .toList();
  }

  private MfdsPublicDeclarationItem toPublicItem(MfdsDeclaration declaration) {
    MfdsPublicImporterItem importer =
        declaration.isImporterLinked()
            ? importerRepository
                .findById(declaration.getImporterId())
                .map(MfdsResponseMapper::toPublicImporterItem)
                .orElse(null)
            : null;
    return MfdsResponseMapper.toPublicDeclarationItem(declaration, importer);
  }
}
