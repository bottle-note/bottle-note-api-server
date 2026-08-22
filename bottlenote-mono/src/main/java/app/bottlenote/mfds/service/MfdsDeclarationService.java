package app.bottlenote.mfds.service;

import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.MFDS_DECLARATION_IMPORTER_LINKED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.MFDS_DECLARATION_IMPORTER_UNLINKED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.MFDS_DECLARATION_STATUS_UPDATED;
import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_ALREADY_LINKED;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_NOT_LINKED;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_IMPORTER_NOT_FOUND;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import app.bottlenote.mfds.dto.request.MfdsDeclarationImporterLinkRequest;
import app.bottlenote.mfds.dto.request.MfdsDeclarationSearchRequest;
import app.bottlenote.mfds.dto.request.MfdsDeclarationStatusRequest;
import app.bottlenote.mfds.dto.response.MfdsDeclarationDetailResponse;
import app.bottlenote.mfds.dto.response.MfdsImporterItem;
import app.bottlenote.mfds.exception.MfdsException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfdsDeclarationService {

  private final MfdsDeclarationRepository declarationRepository;
  private final MfdsImporterRepository importerRepository;

  @Transactional(readOnly = true)
  public GlobalResponse search(MfdsDeclarationSearchRequest request) {
    MfdsDeclarationSearchCriteria criteria = request.toCriteria();
    List<MfdsDeclaration> fetched = declarationRepository.searchByCriteria(criteria);

    boolean hasNext = fetched.size() > criteria.pageSize();
    List<MfdsDeclaration> items =
        hasNext ? fetched.subList(0, criteria.pageSize().intValue()) : fetched;
    Long nextCursor = hasNext ? items.get(items.size() - 1).getId() : null;

    return GlobalResponse.success(
        items.stream().map(MfdsResponseMapper::toDeclarationListItem).toList(),
        createMetaInfo()
            .add("totalElements", declarationRepository.countByCriteria(criteria))
            .add("pageSize", criteria.pageSize())
            .add("hasNext", hasNext)
            .add("nextCursor", nextCursor));
  }

  @Transactional(readOnly = true)
  public MfdsDeclarationDetailResponse getDetail(Long declarationId) {
    MfdsDeclaration declaration = findDeclaration(declarationId);
    MfdsImporterItem importer =
        declaration.isImporterLinked()
            ? importerRepository
                .findById(declaration.getImporterId())
                .map(MfdsResponseMapper::toImporterItem)
                .orElse(null)
            : null;
    return MfdsResponseMapper.toDeclarationDetail(declaration, importer);
  }

  @Transactional
  public AdminResultResponse changeNormalizationStatus(
      Long declarationId, MfdsDeclarationStatusRequest request) {
    MfdsDeclaration declaration = findDeclaration(declarationId);
    declaration.changeNormalizationStatus(
        request.normalizationStatus(), request.reviewedBy(), request.reviewNote());
    declarationRepository.save(declaration);
    return AdminResultResponse.of(MFDS_DECLARATION_STATUS_UPDATED, declarationId);
  }

  /** 수입사를 수동 연결한다. 원장 테이블은 크롤러 소유라 변경하지 않는다. */
  @Transactional
  public AdminResultResponse linkImporter(
      Long declarationId, MfdsDeclarationImporterLinkRequest request) {
    MfdsDeclaration declaration = findDeclaration(declarationId);
    if (declaration.isImporterLinked()) {
      throw new MfdsException(MFDS_DECLARATION_ALREADY_LINKED);
    }
    MfdsImporter importer =
        importerRepository
            .findById(request.importerId())
            .orElseThrow(() -> new MfdsException(MFDS_IMPORTER_NOT_FOUND));

    declaration.linkImporter(importer.getId(), MfdsImporterLinkSource.MANUAL);
    declarationRepository.save(declaration);

    return AdminResultResponse.of(MFDS_DECLARATION_IMPORTER_LINKED, declarationId);
  }

  /** 수입사 연결을 해제한다. 원장 테이블은 크롤러 소유라 변경하지 않는다. */
  @Transactional
  public AdminResultResponse unlinkImporter(Long declarationId) {
    MfdsDeclaration declaration = findDeclaration(declarationId);
    if (!declaration.isImporterLinked()) {
      throw new MfdsException(MFDS_DECLARATION_NOT_LINKED);
    }

    declaration.unlinkImporter();
    declarationRepository.save(declaration);

    return AdminResultResponse.of(MFDS_DECLARATION_IMPORTER_UNLINKED, declarationId);
  }

  private MfdsDeclaration findDeclaration(Long declarationId) {
    return declarationRepository
        .findById(declarationId)
        .orElseThrow(() -> new MfdsException(MFDS_DECLARATION_NOT_FOUND));
  }
}
