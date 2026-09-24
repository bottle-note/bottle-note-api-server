package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_DUPLICATE_TARGET;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_EMPTY_SELECTION;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_PREVIEW_ADMIN_MISMATCH;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_PREVIEW_MISMATCH;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_PREVIEW_NOT_ISSUED;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_SELECTION_LIMIT;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_BULK_TARGET_INVALID;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_DECLARATION_NOT_FOUND;
import static app.bottlenote.mfds.exception.MfdsExceptionCode.MFDS_PRODUCT_IDENTITY_UNAVAILABLE;
import static app.bottlenote.mfds.service.MfdsBulkMatchingJudge.APPLICABLE;
import static app.bottlenote.mfds.service.MfdsBulkMatchingJudge.NO_CHANGE;
import static app.bottlenote.mfds.service.MfdsBulkMatchingJudge.positive;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.mfds.domain.MfdsBulkPreviewIssuance;
import app.bottlenote.mfds.domain.MfdsBulkPreviewIssuanceStore;
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
import app.bottlenote.mfds.exception.MfdsExceptionCode;
import app.bottlenote.mfds.service.MfdsBulkMatchingJudge.Decision;
import app.bottlenote.mfds.service.MfdsBulkMatchingJudge.Signals;
import app.bottlenote.mfds.service.MfdsMatchingService.MatchTarget;
import app.bottlenote.mfds.service.MfdsMatchingService.SelectionAuditContext;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 같은 제품 신고에 주류·증류소·지역을 미리 보고, 고른 대상만 한 트랜잭션으로 확정한다. */
@Service
public class MfdsBulkMatchingService {

  static final int MAX_BULK_DECLARATIONS = 500;
  static final Duration TTL = Duration.ofMinutes(10);

  private final MfdsDeclarationRepository declarationRepository;
  private final MfdsMatchingSelectionRepository selectionRepository;
  private final MfdsMatchingService matchingService;
  private final MfdsBulkPreviewIssuanceStore issuanceStore;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public MfdsBulkMatchingService(
      MfdsDeclarationRepository declarationRepository,
      MfdsMatchingSelectionRepository selectionRepository,
      MfdsMatchingService matchingService,
      MfdsBulkPreviewIssuanceStore issuanceStore) {
    this(
        declarationRepository,
        selectionRepository,
        matchingService,
        issuanceStore,
        Clock.systemDefaultZone());
  }

  MfdsBulkMatchingService(
      MfdsDeclarationRepository declarationRepository,
      MfdsMatchingSelectionRepository selectionRepository,
      MfdsMatchingService matchingService,
      MfdsBulkPreviewIssuanceStore issuanceStore,
      Clock clock) {
    this.declarationRepository = declarationRepository;
    this.selectionRepository = selectionRepository;
    this.matchingService = matchingService;
    this.issuanceStore = issuanceStore;
    this.clock = clock;
  }

  /** 저장소를 바꾸지 않고 같은 제품 그룹을 분류하고, 발급 기록을 Redis에 남긴다. */
  @Transactional(readOnly = true)
  public MfdsBulkMatchingPreviewResponse preview(
      Long sourceId, MfdsBulkMatchingPreviewRequest request, Long adminId) {
    MatchTarget target =
        matchingService.resolveTarget(
            request.alcoholId(), request.distilleryId(), request.regionId());
    List<MfdsDeclaration> rows =
        group(
            declarationRepository.findByProductIdentityKeySha256(identityKey(sourceId)),
            sourceId,
            MFDS_DECLARATION_NOT_FOUND);
    List<Evaluated> group = evaluate(sourceId, rows, target);
    LocalDateTime expiresAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).plus(TTL);
    byte[] tokenBytes = new byte[32];
    secureRandom.nextBytes(tokenBytes);
    String token = HexFormat.of().formatHex(tokenBytes);
    issuanceStore.save(
        new MfdsBulkPreviewIssuance(
            token,
            adminId,
            sourceId,
            request.alcoholId(),
            request.distilleryId(),
            request.regionId(),
            expiresAt,
            stateHash(expiresAt, sourceId, target, group)),
        TTL);
    return new MfdsBulkMatchingPreviewResponse(
        target.alcohol().korName(),
        target.alcohol().engName(),
        target.distilleryId(),
        target.regionId(),
        token,
        expiresAt,
        group.stream().map(Evaluated::previewItem).toList());
  }

  /** 발급 기록의 대상과 잠근 현재 행이 미리보기와 같을 때만 선택 대상을 확정한다. 한 건이라도 맞지 않으면 아무것도 쓰지 않는다. */
  @Transactional
  public MfdsBulkMatchingConfirmResponse confirm(
      Long sourceId, MfdsBulkMatchingConfirmRequest request, Long adminId) {
    List<Long> selectedIds = normalizeSelection(request.declarationIds());
    MfdsBulkPreviewIssuance issuance =
        issuanceStore
            .consume(request.previewToken())
            .filter(issued -> !issued.expiresAt().isBefore(LocalDateTime.now(clock)))
            .orElseThrow(() -> new MfdsException(MFDS_BULK_PREVIEW_NOT_ISSUED));
    if (!adminId.equals(issuance.adminId())) {
      throw new MfdsException(MFDS_BULK_PREVIEW_ADMIN_MISMATCH);
    }
    if (!sourceId.equals(issuance.sourceDeclarationId())) {
      throw new MfdsException(MFDS_BULK_PREVIEW_MISMATCH);
    }
    List<MfdsDeclaration> rows =
        group(
            declarationRepository.findByProductIdentityKeySha256ForUpdate(identityKey(sourceId)),
            sourceId,
            MFDS_BULK_PREVIEW_MISMATCH);
    MatchTarget target =
        matchingService.resolveTarget(
            issuance.alcoholId(), issuance.distilleryId(), issuance.regionId());
    List<Evaluated> group = evaluate(sourceId, rows, target);
    if (!MfdsBulkPreviewHash.matches(
        stateHash(issuance.expiresAt(), sourceId, target, group), issuance.stateHash())) {
      throw new MfdsException(MFDS_BULK_PREVIEW_MISMATCH);
    }
    Map<Long, Evaluated> byId =
        group.stream().collect(Collectors.toMap(e -> e.row().getId(), Function.identity()));
    List<Evaluated> selected = selectedIds.stream().map(byId::get).toList();
    if (selected.stream().anyMatch(e -> e == null || !e.confirmable())) {
      throw new MfdsException(MFDS_BULK_TARGET_INVALID);
    }
    LocalDateTime selectedAt = LocalDateTime.now(clock);
    List<MfdsMatchingConfirmResponse> applied = new ArrayList<>();
    List<Long> unchanged = new ArrayList<>();
    for (Evaluated evaluated : selected) {
      MfdsDeclaration row = evaluated.row();
      if (NO_CHANGE.equals(evaluated.decision().classification())) {
        unchanged.add(row.getId());
        continue;
      }
      matchingService.applyTarget(
          row,
          target,
          new SelectionAuditContext(
              row.getId(), adminId, selectedAt, "BULK_%s:" + sourceId, false));
      applied.add(MfdsMatchingService.toConfirmResponse(row));
    }
    return new MfdsBulkMatchingConfirmResponse(List.copyOf(applied), List.copyOf(unchanged));
  }

  /** 행마다 신호와 관리자 해제 여부를 한 번만 계산해 분류와 상태 해시가 같이 쓴다. */
  private List<Evaluated> evaluate(Long sourceId, List<MfdsDeclaration> rows, MatchTarget target) {
    Set<Long> released = adminReleased(rows);
    Map<Long, Signals> signals = new HashMap<>();
    rows.forEach(row -> signals.put(row.getId(), MfdsBulkMatchingJudge.signals(row)));
    Signals source = signals.get(sourceId);
    return rows.stream()
        .map(
            row -> {
              Signals rowSignals = signals.get(row.getId());
              boolean rowReleased = released.contains(row.getId());
              return new Evaluated(
                  row,
                  rowSignals,
                  rowReleased,
                  MfdsBulkMatchingJudge.decide(source, rowSignals, row, target, rowReleased));
            })
        .toList();
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

  /** 적용 대상과 그룹 행 상태를 순서대로 모아 해시한다. 확정은 잠근 현재 행으로 다시 계산해 비교한다. */
  private static String stateHash(
      LocalDateTime expiresAt, Long sourceId, MatchTarget target, List<Evaluated> group) {
    AlcoholMatchTargetItem alcohol = target.alcohol();
    return MfdsBulkPreviewHash.of(
        Arrays.asList(
            expiresAt.truncatedTo(ChronoUnit.SECONDS).toString(),
            sourceId,
            alcohol.alcoholId(),
            alcohol.korName(),
            alcohol.engName(),
            positive(alcohol.distilleryId()),
            positive(alcohol.regionId()),
            target.distilleryId(),
            target.regionId(),
            group.stream()
                .sorted(Comparator.comparing(e -> e.row().getId()))
                .map(Evaluated::state)
                .toList()));
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

  /** 그룹이 한도 안이고 기준 신고를 포함하는지 확인한다. 기준 신고가 없을 때의 오류는 호출 경로가 정한다. */
  private static List<MfdsDeclaration> group(
      List<MfdsDeclaration> rows, Long sourceId, MfdsExceptionCode missingSource) {
    if (withinLimit(rows).stream().noneMatch(row -> sourceId.equals(row.getId()))) {
      throw new MfdsException(missingSource);
    }
    return rows;
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

  private record Evaluated(
      MfdsDeclaration row, Signals signals, boolean adminReleased, Decision decision) {

    boolean confirmable() {
      return APPLICABLE.equals(decision.classification())
          || NO_CHANGE.equals(decision.classification());
    }

    MfdsBulkMatchingPreviewItem previewItem() {
      return new MfdsBulkMatchingPreviewItem(
          row.getId(),
          row.getRcno(),
          displayName(),
          row.getVolumeMl(),
          row.getImporterBaseName(),
          row.getProcessedDate(),
          decision.classification(),
          decision.reasons(),
          positive(row.getSelectedAlcoholId()),
          positive(row.getSelectedDistilleryId()),
          positive(row.getSelectedRegionId()));
    }

    /** 화면에 보이는 값, 현재 연결, 판정 신호와 동일성 키를 모두 해시에 넣는다. */
    List<Object> state() {
      return Arrays.asList(
          row.getId(),
          row.getRcno(),
          displayName(),
          row.getVolumeMl(),
          row.getImporterBaseName(),
          Objects.toString(row.getProcessedDate(), null),
          positive(row.getSelectedAlcoholId()),
          positive(row.getSelectedDistilleryId()),
          positive(row.getSelectedRegionId()),
          signals,
          adminReleased,
          row.getProductIdentityKeySha256());
    }

    private String displayName() {
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
}
