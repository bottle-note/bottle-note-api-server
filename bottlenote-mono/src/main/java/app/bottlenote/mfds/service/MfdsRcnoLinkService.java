package app.bottlenote.mfds.service;

import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.MFDS_RCNO_LINK_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.MFDS_RCNO_LINK_DELETED;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_IMPORTER_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_RCNO_LINK_DUPLICATE;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_RCNO_LINK_NOT_FOUND;

import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLink;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLinkRepository;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import app.bottlenote.mfds.dto.request.MfdsRcnoLinkCreateRequest;
import app.bottlenote.mfds.dto.response.MfdsRcnoLinkItem;
import app.bottlenote.mfds.exception.MfdsException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfdsRcnoLinkService {

  private final MfdsImporterRcnoLinkRepository rcnoLinkRepository;
  private final MfdsImporterRepository importerRepository;

  /** rcno가 있으면 해당 근거 1건, importerId가 있으면 수입사의 근거 목록을 반환한다. */
  @Transactional(readOnly = true)
  public List<MfdsRcnoLinkItem> search(String rcno, Long importerId) {
    if (rcno != null && !rcno.isBlank()) {
      return rcnoLinkRepository.findByRcno(rcno).map(MfdsResponseMapper::toRcnoLinkItem).stream()
          .toList();
    }
    if (importerId != null) {
      return rcnoLinkRepository.findAllByImporterId(importerId).stream()
          .map(MfdsResponseMapper::toRcnoLinkItem)
          .toList();
    }
    return List.of();
  }

  @Transactional
  public AdminResultResponse create(MfdsRcnoLinkCreateRequest request) {
    if (rcnoLinkRepository.findByRcno(request.rcno()).isPresent()) {
      throw new MfdsException(MFDS_RCNO_LINK_DUPLICATE);
    }
    MfdsImporter importer =
        importerRepository
            .findById(request.importerId())
            .orElseThrow(() -> new MfdsException(MFDS_IMPORTER_NOT_FOUND));

    rcnoLinkRepository.save(
        MfdsImporterRcnoLink.create(
            request.rcno(),
            importer.getId(),
            importer.getBusinessName(),
            MfdsImporterLinkSource.MANUAL));

    return AdminResultResponse.of(MFDS_RCNO_LINK_CREATED, importer.getId());
  }

  @Transactional
  public AdminResultResponse delete(String rcno) {
    MfdsImporterRcnoLink link =
        rcnoLinkRepository
            .findByRcno(rcno)
            .orElseThrow(() -> new MfdsException(MFDS_RCNO_LINK_NOT_FOUND));
    rcnoLinkRepository.deleteByRcno(rcno);
    return AdminResultResponse.of(MFDS_RCNO_LINK_DELETED, link.getImporterId());
  }
}
