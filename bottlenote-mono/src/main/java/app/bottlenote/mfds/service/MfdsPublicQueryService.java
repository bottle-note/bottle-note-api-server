package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.constant.MfdsImporterAdminStatus.ACTIVE;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_IMPORTER_NOT_FOUND;

import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.CursorKeys;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.global.pagination.PaginationException;
import app.bottlenote.global.pagination.PaginationExceptionCode;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import app.bottlenote.mfds.dto.dsl.MfdsPublicAlcoholSearchCriteria;
import app.bottlenote.mfds.dto.dsl.MfdsPublicImporterSearchCriteria;
import app.bottlenote.mfds.dto.request.MfdsPublicAlcoholSearchRequest;
import app.bottlenote.mfds.dto.request.MfdsPublicImporterSearchRequest;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholDetail;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholListItem;
import app.bottlenote.mfds.dto.response.MfdsPublicCountryItem;
import app.bottlenote.mfds.dto.response.MfdsPublicImporterItem;
import app.bottlenote.mfds.exception.MfdsException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfdsPublicQueryService {

  private final MfdsDeclarationRepository declarationRepository;
  private final MfdsImporterRepository importerRepository;
  private final HmacCursorCodec cursorCodec;

  @Transactional(readOnly = true)
  public KeysetPageResponse<List<MfdsPublicAlcoholListItem>> searchAlcohols(
      MfdsPublicAlcoholSearchRequest request) {
    MfdsPublicAlcoholSearchCriteria base = MfdsPublicAlcoholSearchCriteria.of(request, null, null);
    String context = base.cursorContext();
    LocalDate cursorDate = null;
    Long cursorId = null;
    if (request.cursor() != null) {
      CursorClaims claims = cursorCodec.verify(request.cursor(), context);
      cursorDate = parseDate(CursorKeys.optional(claims, "processedDate"));
      cursorId = CursorKeys.requireLong(claims, "id");
    }
    MfdsPublicAlcoholSearchCriteria criteria =
        MfdsPublicAlcoholSearchCriteria.of(request, cursorDate, cursorId);
    List<MfdsDeclaration> fetched = declarationRepository.searchPublicAlcohols(criteria);
    KeysetPagination.PageSlice<MfdsDeclaration> slice =
        KeysetPagination.fromOverflow(
            fetched, request.size(), last -> encodeAlcoholCursor(context, last));
    return KeysetPageResponse.of(
        slice.items().stream().map(MfdsResponseMapper::toPublicAlcoholListItem).toList(),
        slice.pagination());
  }

  @Transactional(readOnly = true)
  public MfdsPublicAlcoholDetail getAlcohol(Long id) {
    MfdsDeclaration declaration =
        declarationRepository
            .findById(id)
            .orElseThrow(() -> new MfdsException(MFDS_DECLARATION_NOT_FOUND));
    MfdsPublicImporterItem importer = publicImporterOf(declaration.getImporterId());
    return MfdsResponseMapper.toPublicAlcoholDetail(declaration, importer);
  }

  @Transactional(readOnly = true)
  public KeysetPageResponse<List<MfdsPublicImporterItem>> searchImporters(
      MfdsPublicImporterSearchRequest request) {
    MfdsPublicImporterSearchCriteria base = MfdsPublicImporterSearchCriteria.of(request, null);
    String context = base.cursorContext();
    Long cursorId = null;
    if (request.cursor() != null) {
      CursorClaims claims = cursorCodec.verify(request.cursor(), context);
      cursorId = CursorKeys.requireLong(claims, "id");
    }
    MfdsPublicImporterSearchCriteria criteria =
        MfdsPublicImporterSearchCriteria.of(request, cursorId);
    List<MfdsImporter> fetched = importerRepository.searchPublicImporters(criteria);
    KeysetPagination.PageSlice<MfdsImporter> slice =
        KeysetPagination.fromOverflow(
            fetched,
            request.size(),
            last -> cursorCodec.encode(context, Map.of("id", String.valueOf(last.getId()))));
    return KeysetPageResponse.of(
        slice.items().stream().map(MfdsResponseMapper::toPublicImporterItem).toList(),
        slice.pagination());
  }

  @Transactional(readOnly = true)
  public MfdsPublicImporterItem getImporter(Long importerId) {
    return importerRepository.findAllByIdInAndAdminStatus(List.of(importerId), ACTIVE).stream()
        .findFirst()
        .map(MfdsResponseMapper::toPublicImporterItem)
        .orElseThrow(() -> new MfdsException(MFDS_IMPORTER_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<MfdsPublicCountryItem> listCountries() {
    return declarationRepository.findExportCountries();
  }

  private MfdsPublicImporterItem publicImporterOf(Long importerId) {
    if (importerId == null) {
      return null;
    }
    return importerRepository.findAllByIdInAndAdminStatus(List.of(importerId), ACTIVE).stream()
        .findFirst()
        .map(MfdsResponseMapper::toPublicImporterItem)
        .orElse(null);
  }

  private String encodeAlcoholCursor(String context, MfdsDeclaration last) {
    Map<String, String> keys = new HashMap<>();
    keys.put("id", String.valueOf(last.getId()));
    if (last.getProcessedDate() != null) {
      keys.put("processedDate", last.getProcessedDate().toString());
    }
    return cursorCodec.encode(context, keys);
  }

  private static LocalDate parseDate(String value) {
    if (value == null) {
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (RuntimeException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }
}
