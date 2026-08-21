package app.bottlenote.mfds.service;

import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.MFDS_IMPORTER_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.MFDS_IMPORTER_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.MFDS_IMPORTER_UPDATED;
import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_IMPORTER_DUPLICATE_CODE;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_IMPORTER_HAS_DECLARATIONS;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_IMPORTER_HAS_RCNO_LINKS;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_IMPORTER_NOT_FOUND;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLinkRepository;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import app.bottlenote.mfds.dto.dsl.MfdsImporterSearchCriteria;
import app.bottlenote.mfds.dto.request.MfdsImporterCreateRequest;
import app.bottlenote.mfds.dto.request.MfdsImporterSearchRequest;
import app.bottlenote.mfds.dto.request.MfdsImporterUpdateRequest;
import app.bottlenote.mfds.dto.response.MfdsImporterItem;
import app.bottlenote.mfds.exception.MfdsException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfdsImporterService {

  private final MfdsImporterRepository importerRepository;
  private final MfdsDeclarationRepository declarationRepository;
  private final MfdsImporterRcnoLinkRepository rcnoLinkRepository;

  @Transactional(readOnly = true)
  public GlobalResponse search(MfdsImporterSearchRequest request) {
    MfdsImporterSearchCriteria criteria = request.toCriteria();
    List<MfdsImporter> fetched = importerRepository.searchByCriteria(criteria);

    boolean hasNext = fetched.size() > criteria.pageSize();
    List<MfdsImporter> items =
        hasNext ? fetched.subList(0, criteria.pageSize().intValue()) : fetched;
    Long nextCursor = hasNext ? items.get(items.size() - 1).getId() : null;

    return GlobalResponse.success(
        items.stream().map(MfdsResponseMapper::toImporterItem).toList(),
        createMetaInfo()
            .add("totalElements", importerRepository.countByCriteria(criteria))
            .add("pageSize", criteria.pageSize())
            .add("hasNext", hasNext)
            .add("nextCursor", nextCursor));
  }

  @Transactional(readOnly = true)
  public MfdsImporterItem getDetail(Long importerId) {
    return MfdsResponseMapper.toImporterItem(findImporter(importerId));
  }

  @Transactional
  public AdminResultResponse create(MfdsImporterCreateRequest request) {
    if (importerRepository.findByOfficialBusinessCode(request.officialBusinessCode()).isPresent()) {
      throw new MfdsException(MFDS_IMPORTER_DUPLICATE_CODE);
    }

    MfdsImporter importer =
        MfdsImporter.create(
            request.officialBusinessCode(),
            request.licenseNo(),
            request.businessName(),
            request.representativeName(),
            request.sourceListUrl(),
            request.description(),
            request.adminNote(),
            request.adminStatus());

    MfdsImporter saved = importerRepository.save(importer);
    return AdminResultResponse.of(MFDS_IMPORTER_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse update(Long importerId, MfdsImporterUpdateRequest request) {
    MfdsImporter importer = findImporter(importerId);
    importer.update(
        request.businessName(), request.description(), request.adminNote(), request.adminStatus());
    importerRepository.save(importer);
    return AdminResultResponse.of(MFDS_IMPORTER_UPDATED, importerId);
  }

  @Transactional
  public AdminResultResponse delete(Long importerId) {
    MfdsImporter importer = findImporter(importerId);

    if (declarationRepository.existsByImporterId(importerId)) {
      throw new MfdsException(MFDS_IMPORTER_HAS_DECLARATIONS);
    }
    if (rcnoLinkRepository.countByImporterId(importerId) > 0) {
      throw new MfdsException(MFDS_IMPORTER_HAS_RCNO_LINKS);
    }

    importerRepository.delete(importer);
    return AdminResultResponse.of(MFDS_IMPORTER_DELETED, importerId);
  }

  private MfdsImporter findImporter(Long importerId) {
    return importerRepository
        .findById(importerId)
        .orElseThrow(() -> new MfdsException(MFDS_IMPORTER_NOT_FOUND));
  }
}
