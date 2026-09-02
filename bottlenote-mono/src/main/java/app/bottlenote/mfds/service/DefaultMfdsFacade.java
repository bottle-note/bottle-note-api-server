package app.bottlenote.mfds.service;

import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import app.bottlenote.mfds.facade.MfdsFacade;
import app.bottlenote.mfds.facade.payload.MfdsPublicDeclarationItem;
import app.bottlenote.mfds.facade.payload.MfdsPublicImporterItem;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    List<MfdsDeclaration> declarations =
        declarationRepository.findAllBySelectedAlcoholId(alcoholId);
    if (declarations.isEmpty()) {
      return List.of();
    }
    Map<Long, MfdsImporter> importersById = loadImporters(declarations);
    return declarations.stream()
        .map(declaration -> toPublicItem(declaration, importersById))
        .toList();
  }

  private Map<Long, MfdsImporter> loadImporters(Collection<MfdsDeclaration> declarations) {
    List<Long> importerIds =
        declarations.stream()
            .map(MfdsDeclaration::getImporterId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (importerIds.isEmpty()) {
      return Map.of();
    }
    return importerRepository.findAllByIdIn(importerIds).stream()
        .collect(Collectors.toMap(MfdsImporter::getId, Function.identity()));
  }

  private MfdsPublicDeclarationItem toPublicItem(
      MfdsDeclaration declaration, Map<Long, MfdsImporter> importersById) {
    MfdsPublicImporterItem importer = null;
    if (declaration.isImporterLinked()) {
      MfdsImporter linked = importersById.get(declaration.getImporterId());
      if (linked != null) {
        importer = MfdsResponseMapper.toPublicImporterItem(linked);
      }
    }
    return MfdsResponseMapper.toPublicDeclarationItem(declaration, importer);
  }
}
