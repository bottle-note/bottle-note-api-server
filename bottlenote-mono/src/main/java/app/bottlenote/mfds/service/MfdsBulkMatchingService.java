package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_DUPLICATE_TARGET;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_EMPTY_SELECTION;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_PREVIEW_ADMIN_MISMATCH;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_PREVIEW_EXPIRED;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_PREVIEW_MISMATCH;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_PREVIEW_NOT_ISSUED;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_SELECTION_LIMIT;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_TARGET_INVALID;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_PRODUCT_IDENTITY_UNAVAILABLE;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_ALCOHOL_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_DISTILLERY_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_SELECTED_REGION_NOT_FOUND;
import static app.bottlenote.mfds.service.MfdsBulkMatchingJudge.APPLICABLE;
import static app.bottlenote.mfds.service.MfdsBulkMatchingJudge.NO_CHANGE;

import app.bottlenote.alcohols.facade.AlcoholMatchTargetFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.domain.MfdsBulkPreviewIssuance;
import app.bottlenote.mfds.domain.MfdsBulkPreviewIssuanceStore;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsMatchingCandidate;
import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.domain.MfdsMatchingSelectionRepository;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingConfirmRequest;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingPreviewRequest;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingConfirmItem;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingConfirmResponse;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewItem;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewResponse;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.service.MfdsBulkMatchingJudge.Decision;
import app.bottlenote.mfds.service.MfdsBulkMatchingToken.Applied;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 같은 제품 신고에 주류·증류소·지역을 미리 보고, 고른 대상만 한 트랜잭션으로 확정한다. */
@Service
public class MfdsBulkMatchingService {

  static final int MAX_BULK_DECLARATIONS = 500;

  private final MfdsDeclarationRepository declarationRepository;
  private final AlcoholMatchTargetFacade alcoholMatchTargetFacade;
  private final MfdsMatchingSelectionRepository selectionRepository;
  private final MfdsMatchingHistoryService historyService;
  private final MfdsBulkPreviewIssuanceStore issuanceStore;
  private final MfdsBulkLockGate lockGate;
  private final MfdsBulkSaveGuard saveGuard;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public MfdsBulkMatchingService(
      MfdsDeclarationRepository declarationRepository,
      AlcoholMatchTargetFacade alcoholMatchTargetFacade,
      MfdsMatchingSelectionRepository selectionRepository,
      MfdsMatchingHistoryService historyService,
      MfdsBulkPreviewIssuanceStore issuanceStore,
      MfdsBulkLockGate lockGate,
      MfdsBulkSaveGuard saveGuard) {
    this(
        declarationRepository,
        alcoholMatchTargetFacade,
        selectionRepository,
        historyService,
        issuanceStore,
        lockGate,
        saveGuard,
        Clock.systemDefaultZone());
  }

  MfdsBulkMatchingService(
      MfdsDeclarationRepository declarationRepository,
      AlcoholMatchTargetFacade alcoholMatchTargetFacade,
      MfdsMatchingSelectionRepository selectionRepository,
      MfdsMatchingHistoryService historyService,
      MfdsBulkPreviewIssuanceStore issuanceStore,
      MfdsBulkLockGate lockGate,
      MfdsBulkSaveGuard saveGuard,
      Clock clock) {
    this.declarationRepository = declarationRepository;
    this.alcoholMatchTargetFacade = alcoholMatchTargetFacade;
    this.selectionRepository = selectionRepository;
    this.historyService = historyService;
    this.issuanceStore = issuanceStore;
    this.lockGate = lockGate;
    this.saveGuard = saveGuard;
    this.clock = clock;
  }

  /** 저장소를 바꾸지 않고 같은 제품 그룹을 분류하고, 발급 기록을 Redis에 남긴다. */
  @Transactional(readOnly = true)
  public MfdsBulkMatchingPreviewResponse preview(
      MfdsBulkMatchingPreviewRequest request, Long adminId) {
    Applied applied =
        resolveApplied(request.alcoholId(), request.distilleryId(), request.regionId());
    List<MfdsDeclaration> rows = readableGroup(request.sourceDeclarationId());
    LocalDateTime expiresAt =
        LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).plus(MfdsBulkMatchingToken.TTL);
    String stateHash =
        MfdsBulkMatchingToken.sign(
            expiresAt, request.sourceDeclarationId(), applied, rows, releaseFlags(rows));
    String token = randomToken();
    issuanceStore.save(
        new MfdsBulkPreviewIssuance(
            token,
            adminId,
            request.sourceDeclarationId(),
            request.alcoholId(),
            request.distilleryId(),
            request.regionId(),
            expiresAt,
            stateHash),
        MfdsBulkMatchingToken.TTL);
    return response(request.sourceDeclarationId(), applied, rows, expiresAt, token);
  }

  /** 미리보기 검증값이 아직 유효할 때만 선택 대상을 확정한다. 한 건이라도 맞지 않으면 아무것도 쓰지 않는다. */
  @Transactional
  public MfdsBulkMatchingConfirmResponse confirm(
      MfdsBulkMatchingConfirmRequest request, Long adminId) {
    List<Long> selectedIds = normalizeSelection(request.declarationIds());
    LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    if (request.previewExpiresAt().truncatedTo(ChronoUnit.SECONDS).isBefore(now)) {
      throw new MfdsException(MFDS_BULK_PREVIEW_EXPIRED);
    }
    MfdsBulkPreviewIssuance issuance =
        issuanceStore
            .consume(request.previewToken())
            .orElseThrow(() -> new MfdsException(MFDS_BULK_PREVIEW_NOT_ISSUED));
    if (!adminId.equals(issuance.adminId())) {
      throw new MfdsException(MFDS_BULK_PREVIEW_ADMIN_MISMATCH);
    }
    if (!request.previewExpiresAt().truncatedTo(ChronoUnit.SECONDS).equals(issuance.expiresAt())
        || !request.sourceDeclarationId().equals(issuance.sourceDeclarationId())
        || !request.alcoholId().equals(issuance.alcoholId())
        || !Objects.equals(request.distilleryId(), issuance.distilleryId())
        || !Objects.equals(request.regionId(), issuance.regionId())) {
      throw new MfdsException(MFDS_BULK_PREVIEW_MISMATCH);
    }
    byte[] key = identityKey(request.sourceDeclarationId());
    lockGate.beforeGroupLock(request.sourceDeclarationId());
    List<MfdsDeclaration> rows = declarationRepository.findByProductIdentityKeySha256ForUpdate(key);
    ensureWithinLimit(rows.size());
    if (rows.stream().noneMatch(row -> request.sourceDeclarationId().equals(row.getId()))) {
      throw new MfdsException(MFDS_BULK_PREVIEW_MISMATCH);
    }
    Applied applied =
        resolveApplied(request.alcoholId(), request.distilleryId(), request.regionId());
    Map<Long, Boolean> released = releaseFlags(rows);
    String expected =
        MfdsBulkMatchingToken.sign(
            issuance.expiresAt(), request.sourceDeclarationId(), applied, rows, released);
    if (!MfdsBulkMatchingToken.matches(expected, issuance.stateHash())) {
      throw new MfdsException(MFDS_BULK_PREVIEW_MISMATCH);
    }
    Map<Long, Decision> decisions = decisions(request.sourceDeclarationId(), rows, applied);
    List<MfdsDeclaration> selected = new ArrayList<>();
    for (Long id : selectedIds) {
      MfdsDeclaration declaration =
          rows.stream().filter(row -> id.equals(row.getId())).findFirst().orElse(null);
      Decision decision = declaration == null ? null : decisions.get(id);
      if (declaration == null
          || decision == null
          || (!APPLICABLE.equals(decision.classification())
              && !NO_CHANGE.equals(decision.classification()))) {
        throw new MfdsException(MFDS_BULK_TARGET_INVALID);
      }
      selected.add(declaration);
    }
    LocalDateTime selectedAt = LocalDateTime.now(clock);
    List<MfdsBulkMatchingConfirmItem> items = new ArrayList<>();
    int appliedCount = 0;
    int unchangedCount = 0;
    int saveIndex = 0;
    SelectionAudit audit = new SelectionAudit(request.sourceDeclarationId(), adminId, selectedAt);
    for (MfdsDeclaration declaration : selected) {
      Decision decision = decisions.get(declaration.getId());
      if (NO_CHANGE.equals(decision.classification())) {
        unchangedCount++;
        items.add(item(declaration, "NO_CHANGE"));
        continue;
      }
      saveGuard.beforeSave(saveIndex, declaration.getId());
      apply(declaration, request, applied, audit);
      declarationRepository.save(declaration);
      saveIndex++;
      appliedCount++;
      items.add(item(declaration, "APPLIED"));
    }
    return new MfdsBulkMatchingConfirmResponse(
        request.sourceDeclarationId(),
        applied.alcoholId(),
        applied.alcoholNameKo(),
        applied.alcoholNameEn(),
        applied.distilleryId(),
        applied.regionId(),
        appliedCount,
        unchangedCount,
        List.copyOf(items));
  }

  private void apply(
      MfdsDeclaration declaration,
      MfdsBulkMatchingConfirmRequest request,
      Applied applied,
      SelectionAudit audit) {
    List<MfdsMatchingCandidate> candidates = historyService.findCandidates(declaration);
    MfdsMatchSelectionSource alcoholSource =
        selectionSource(hasCandidate(candidates, "ALCOHOL", applied.alcoholId()));
    MfdsMatchSelectionSource distillerySource =
        referenceSource(
            request.distilleryId(),
            applied.distilleryId(),
            hasCandidate(candidates, "DISTILLERY", applied.distilleryId()));
    MfdsMatchSelectionSource regionSource =
        referenceSource(
            request.regionId(),
            applied.regionId(),
            hasCandidate(candidates, "REGION", applied.regionId()));
    declaration.confirmMatching(
        applied.alcoholId(),
        alcoholSource,
        applied.distilleryId(),
        distillerySource,
        applied.regionId(),
        regionSource);
    declaration.applyMatchedAlcoholName(applied.alcoholNameKo(), applied.alcoholNameEn());
    recordSelection(declaration.getId(), "ALCOHOL", applied.alcoholId(), alcoholSource, audit);
    recordReference(
        declaration.getId(), "DISTILLERY", applied.distilleryId(), distillerySource, audit);
    recordReference(declaration.getId(), "REGION", applied.regionId(), regionSource, audit);
  }

  private void recordReference(
      Long declarationId,
      String targetType,
      Long targetId,
      MfdsMatchSelectionSource source,
      SelectionAudit audit) {
    if (targetId != null && source != null) {
      recordSelection(declarationId, targetType, targetId, source, audit);
    }
  }

  private void recordSelection(
      Long declarationId,
      String targetType,
      Long targetId,
      MfdsMatchSelectionSource source,
      SelectionAudit audit) {
    selectionRepository.save(
        MfdsMatchingSelection.adminSelect(
            declarationId,
            targetType,
            targetId,
            "BULK_" + source.name() + ":" + audit.sourceDeclarationId(),
            audit.adminId(),
            audit.selectedAt()));
  }

  private record SelectionAudit(Long sourceDeclarationId, Long adminId, LocalDateTime selectedAt) {}

  private MfdsBulkMatchingPreviewResponse response(
      Long sourceDeclarationId,
      Applied applied,
      List<MfdsDeclaration> rows,
      LocalDateTime expiresAt,
      String issuedToken) {
    MfdsDeclaration source = findRow(rows, sourceDeclarationId);
    List<MfdsBulkMatchingPreviewItem> items = new ArrayList<>();
    int applicable = 0;
    int unchanged = 0;
    int review = 0;
    int conflict = 0;
    for (MfdsDeclaration row : rows) {
      Decision decision = decisionFor(source, row, applied);
      switch (decision.classification()) {
        case APPLICABLE -> applicable++;
        case NO_CHANGE -> unchanged++;
        case MfdsBulkMatchingJudge.NEEDS_REVIEW -> review++;
        case MfdsBulkMatchingJudge.CONFLICT -> conflict++;
        default -> throw new IllegalStateException(decision.classification());
      }
      items.add(
          new MfdsBulkMatchingPreviewItem(
              row.getId(),
              row.getRcno(),
              MfdsBulkMatchingToken.displayName(row),
              row.getVolumeMl(),
              row.getImporterBaseName(),
              row.getProcessedDate(),
              decision.classification(),
              decision.reasons(),
              MfdsBulkMatchingJudge.positive(row.getSelectedAlcoholId()),
              MfdsBulkMatchingJudge.positive(row.getSelectedDistilleryId()),
              MfdsBulkMatchingJudge.positive(row.getSelectedRegionId())));
    }
    return new MfdsBulkMatchingPreviewResponse(
        sourceDeclarationId,
        applied.alcoholId(),
        applied.alcoholNameKo(),
        applied.alcoholNameEn(),
        applied.distilleryId(),
        applied.regionId(),
        issuedToken,
        expiresAt,
        applicable,
        unchanged,
        review,
        conflict,
        List.copyOf(items));
  }

  private Map<Long, Decision> decisions(
      Long sourceId, List<MfdsDeclaration> rows, Applied applied) {
    MfdsDeclaration source = findRow(rows, sourceId);
    Map<Long, Decision> decisions = new LinkedHashMap<>();
    for (MfdsDeclaration row : rows) {
      decisions.put(row.getId(), decisionFor(source, row, applied));
    }
    return decisions;
  }

  private Decision decisionFor(MfdsDeclaration source, MfdsDeclaration row, Applied applied) {
    return MfdsBulkMatchingJudge.decide(
        source,
        row,
        applied.alcoholId(),
        applied.distilleryId(),
        applied.regionId(),
        adminReleased(row.getId()));
  }

  private boolean adminReleased(Long declarationId) {
    return selectionRepository
        .findByDeclarationIdOrderBySelectedAtDescIdDesc(declarationId)
        .stream()
        .filter(selection -> "ALCOHOL".equals(selection.getTargetType()))
        .filter(selection -> "ADMIN".equals(selection.getSelectionSource()))
        .findFirst()
        .map(selection -> "REVOKE".equals(selection.getAction()))
        .orElse(false);
  }

  private Map<Long, Boolean> releaseFlags(List<MfdsDeclaration> rows) {
    Map<Long, Boolean> flags = new LinkedHashMap<>();
    for (MfdsDeclaration row : rows) {
      flags.put(row.getId(), adminReleased(row.getId()));
    }
    return flags;
  }

  private static void ensureWithinLimit(int size) {
    if (size > MAX_BULK_DECLARATIONS) {
      throw new MfdsException(MFDS_BULK_SELECTION_LIMIT);
    }
  }

  private List<MfdsDeclaration> readableGroup(Long sourceDeclarationId) {
    byte[] key = identityKey(sourceDeclarationId);
    List<MfdsDeclaration> rows = declarationRepository.findByProductIdentityKeySha256(key);
    ensureWithinLimit(rows.size());
    findRow(rows, sourceDeclarationId);
    return rows;
  }

  private byte[] identityKey(Long sourceDeclarationId) {
    if (declarationRepository.findDeclarationId(sourceDeclarationId) == null) {
      throw new MfdsException(MFDS_DECLARATION_NOT_FOUND);
    }
    byte[] key = declarationRepository.findProductIdentityKeySha256ById(sourceDeclarationId);
    if (key == null || key.length == 0) {
      throw new MfdsException(MFDS_PRODUCT_IDENTITY_UNAVAILABLE);
    }
    return key.clone();
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static MfdsDeclaration findRow(List<MfdsDeclaration> rows, Long id) {
    return rows.stream()
        .filter(row -> id.equals(row.getId()))
        .findFirst()
        .orElseThrow(() -> new MfdsException(MFDS_DECLARATION_NOT_FOUND));
  }

  private Applied resolveApplied(
      Long alcoholId, Long requestedDistilleryId, Long requestedRegionId) {
    AlcoholMatchTargetItem alcohol =
        alcoholMatchTargetFacade.findAlcoholTargetsByIds(List.of(alcoholId)).stream()
            .findFirst()
            .orElseThrow(() -> new MfdsException(MFDS_SELECTED_ALCOHOL_NOT_FOUND));
    if (requestedDistilleryId != null
        && !alcoholMatchTargetFacade.existsDistillery(requestedDistilleryId)) {
      throw new MfdsException(MFDS_SELECTED_DISTILLERY_NOT_FOUND);
    }
    if (requestedRegionId != null && !alcoholMatchTargetFacade.existsRegion(requestedRegionId)) {
      throw new MfdsException(MFDS_SELECTED_REGION_NOT_FOUND);
    }
    Long distilleryId =
        requestedDistilleryId != null
            ? requestedDistilleryId
            : MfdsBulkMatchingJudge.positive(alcohol.distilleryId());
    Long regionId =
        requestedRegionId != null
            ? requestedRegionId
            : MfdsBulkMatchingJudge.positive(alcohol.regionId());
    return new Applied(
        alcohol.alcoholId(),
        alcohol.korName(),
        alcohol.engName(),
        MfdsBulkMatchingJudge.positive(alcohol.distilleryId()),
        MfdsBulkMatchingJudge.positive(alcohol.regionId()),
        distilleryId,
        regionId);
  }

  private static List<Long> normalizeSelection(List<Long> declarationIds) {
    if (declarationIds == null
        || declarationIds.isEmpty()
        || declarationIds.stream().anyMatch(Objects::isNull)) {
      throw new MfdsException(MFDS_BULK_EMPTY_SELECTION);
    }
    if (declarationIds.stream().anyMatch(id -> id <= 0)) {
      throw new MfdsException(MFDS_BULK_TARGET_INVALID);
    }
    if (declarationIds.size() != new HashSet<>(declarationIds).size()) {
      throw new MfdsException(MFDS_BULK_DUPLICATE_TARGET);
    }
    ensureWithinLimit(declarationIds.size());
    return declarationIds.stream().sorted().toList();
  }

  private static boolean hasCandidate(
      List<MfdsMatchingCandidate> candidates, String type, Long id) {
    return id != null
        && candidates.stream()
            .anyMatch(
                candidate ->
                    type.equals(candidate.getTargetType()) && id.equals(candidate.getTargetId()));
  }

  private static MfdsMatchSelectionSource selectionSource(boolean fromCandidate) {
    return fromCandidate ? MfdsMatchSelectionSource.CANDIDATE : MfdsMatchSelectionSource.MANUAL;
  }

  private static MfdsMatchSelectionSource referenceSource(
      Long requestedId, Long selectedId, boolean fromCandidate) {
    if (selectedId == null) {
      return null;
    }
    return requestedId != null
        ? selectionSource(fromCandidate)
        : MfdsMatchSelectionSource.ALCOHOL_PROPAGATED;
  }

  private static MfdsBulkMatchingConfirmItem item(MfdsDeclaration declaration, String outcome) {
    return new MfdsBulkMatchingConfirmItem(
        declaration.getId(),
        outcome,
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholMatchDecision(),
        declaration.getSelectedDistilleryId(),
        declaration.getDistilleryMatchSource(),
        declaration.getSelectedRegionId(),
        declaration.getRegionMatchSource());
  }
}
