package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_DUPLICATE_TARGET;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_EMPTY_SELECTION;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_SELECTION_LIMIT;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_TARGET_INVALID;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_PRODUCT_IDENTITY_UNAVAILABLE;
import static app.bottlenote.mfds.service.MfdsBulkMatchingJudge.positive;

import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.domain.MfdsMatchingSelectionRepository;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingConfirmRequest;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingPreviewRequest;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingConfirmResponse;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewItem;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewResponse;
import app.bottlenote.mfds.dto.response.MfdsMatchingConfirmResponse;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.service.MfdsBulkMatchingJudge.Decision;
import app.bottlenote.mfds.service.MfdsBulkMatchingJudge.Signals;
import app.bottlenote.mfds.service.MfdsMatchingService.MatchTarget;
import app.bottlenote.mfds.service.MfdsMatchingService.SelectionAuditContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 같은 제품 신고의 연결 상태를 미리 보여 주고, 고른 신고에 주류·증류소·지역을 그대로 반영한다. */
@Service
public class MfdsBulkMatchingService {

  static final int MAX_BULK_DECLARATIONS = 500;

  private final MfdsDeclarationRepository declarationRepository;
  private final MfdsMatchingSelectionRepository selectionRepository;
  private final MfdsMatchingService matchingService;
  private final Clock clock;

  @Autowired
  public MfdsBulkMatchingService(
      MfdsDeclarationRepository declarationRepository,
      MfdsMatchingSelectionRepository selectionRepository,
      MfdsMatchingService matchingService) {
    this(declarationRepository, selectionRepository, matchingService, Clock.systemDefaultZone());
  }

  MfdsBulkMatchingService(
      MfdsDeclarationRepository declarationRepository,
      MfdsMatchingSelectionRepository selectionRepository,
      MfdsMatchingService matchingService,
      Clock clock) {
    this.declarationRepository = declarationRepository;
    this.selectionRepository = selectionRepository;
    this.matchingService = matchingService;
    this.clock = clock;
  }

  /** 저장소를 바꾸지 않고 같은 제품 그룹의 현재 연결과 차이를 보여 준다. 분류는 안내용이며 확정을 막지 않는다. */
  @Transactional(readOnly = true)
  public MfdsBulkMatchingPreviewResponse preview(
      Long sourceId, MfdsBulkMatchingPreviewRequest request) {
    MatchTarget target =
        matchingService.resolveTarget(
            request.alcoholId(), request.distilleryId(), request.regionId());
    List<MfdsDeclaration> rows =
        withinLimit(declarationRepository.findByProductIdentityKeySha256(identityKey(sourceId)));
    if (rows.stream().noneMatch(row -> sourceId.equals(row.getId()))) {
      throw new MfdsException(MFDS_DECLARATION_NOT_FOUND);
    }
    Set<Long> released = adminReleased(rows);
    Map<Long, Signals> signals = new HashMap<>();
    rows.forEach(row -> signals.put(row.getId(), MfdsBulkMatchingJudge.signals(row)));
    Signals source = signals.get(sourceId);
    List<MfdsBulkMatchingPreviewItem> items =
        rows.stream()
            .map(
                row ->
                    previewItem(
                        row,
                        MfdsBulkMatchingJudge.decide(
                            source,
                            signals.get(row.getId()),
                            row,
                            target,
                            released.contains(row.getId()))))
            .toList();
    return new MfdsBulkMatchingPreviewResponse(
        target.alcohol().korName(),
        target.alcohol().engName(),
        target.distilleryId(),
        target.regionId(),
        items);
  }

  /** 고른 신고에 적용값을 덮어쓴다. 이미 같은 값이 연결된 신고는 쓰지 않는다. 그룹 소속과 미리보기 분류는 검사하지 않는다. */
  @Transactional
  public MfdsBulkMatchingConfirmResponse confirm(
      Long sourceId, MfdsBulkMatchingConfirmRequest request, Long adminId) {
    List<Long> selectedIds = normalizeSelection(request.declarationIds());
    MatchTarget target =
        matchingService.resolveTarget(
            request.alcoholId(), request.distilleryId(), request.regionId());
    List<MfdsDeclaration> rows = declarationRepository.findByIdInForUpdate(selectedIds);
    if (rows.size() != selectedIds.size()) {
      throw new MfdsException(MFDS_DECLARATION_NOT_FOUND);
    }
    LocalDateTime selectedAt = LocalDateTime.now(clock);
    List<MfdsMatchingConfirmResponse> applied = new ArrayList<>();
    List<Long> unchanged = new ArrayList<>();
    for (MfdsDeclaration row : rows) {
      if (alreadyLinked(row, target)) {
        unchanged.add(row.getId());
        continue;
      }
      matchingService.applyTarget(
          row,
          target,
          new SelectionAuditContext(row.getId(), adminId, selectedAt, "BULK_%s:" + sourceId));
      applied.add(MfdsMatchingService.toConfirmResponse(row));
    }
    return new MfdsBulkMatchingConfirmResponse(List.copyOf(applied), List.copyOf(unchanged));
  }

  private static boolean alreadyLinked(MfdsDeclaration row, MatchTarget target) {
    return Objects.equals(positive(row.getSelectedAlcoholId()), target.alcohol().alcoholId())
        && Objects.equals(positive(row.getSelectedDistilleryId()), positive(target.distilleryId()))
        && Objects.equals(positive(row.getSelectedRegionId()), positive(target.regionId()));
  }

  /** 그룹의 선택 이력을 최신순으로 한 번에 읽고, 신고별 최신 관리자 주류 이력이 해제인 신고를 고른다. */
  private Set<Long> adminReleased(List<MfdsDeclaration> rows) {
    Set<Long> seen = new HashSet<>();
    Set<Long> released = new HashSet<>();
    for (MfdsMatchingSelection selection :
        selectionRepository.findByDeclarationIdInOrderBySelectedAtDescIdDesc(
            rows.stream().map(MfdsDeclaration::getId).toList())) {
      if ("ALCOHOL".equals(selection.getTargetType())
          && "ADMIN".equals(selection.getSelectionSource())
          && seen.add(selection.getDeclarationId())
          && "REVOKE".equals(selection.getAction())) {
        released.add(selection.getDeclarationId());
      }
    }
    return released;
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

  private static <T> List<T> withinLimit(List<T> values) {
    if (values.size() > MAX_BULK_DECLARATIONS) {
      throw new MfdsException(MFDS_BULK_SELECTION_LIMIT);
    }
    return values;
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
    return withinLimit(declarationIds).stream().sorted().toList();
  }

  private static MfdsBulkMatchingPreviewItem previewItem(MfdsDeclaration row, Decision decision) {
    return new MfdsBulkMatchingPreviewItem(
        row.getId(),
        row.getRcno(),
        displayName(row),
        row.getVolumeMl(),
        row.getImporterBaseName(),
        row.getProcessedDate(),
        decision.classification(),
        decision.reasons(),
        positive(row.getSelectedAlcoholId()),
        positive(row.getSelectedDistilleryId()),
        positive(row.getSelectedRegionId()));
  }

  private static String displayName(MfdsDeclaration row) {
    return Stream.of(
            row.getSkuDisplayNameKo(),
            row.getBaseProductNameKo(),
            row.getAlcoholNameKo(),
            row.getNameSearchKeyKo())
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse(row.getRcno());
  }
}
