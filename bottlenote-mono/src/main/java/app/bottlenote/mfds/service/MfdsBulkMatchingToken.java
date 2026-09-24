package app.bottlenote.mfds.service;

import app.bottlenote.mfds.domain.MfdsDeclaration;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * 미리보기 발급 기록 안의 내부 상태 해시. 클라이언트에 주는 난수 previewToken이 아니다. 확정은 잠근 현재 행으로 이 해시를 다시 계산해 Redis에 저장된 값과
 * 비교한다.
 */
final class MfdsBulkMatchingToken {

  static final Duration TTL = Duration.ofMinutes(10);
  private static final String VERSION = "mfds-bulk-preview-v1";
  private static final ObjectMapper MAPPER =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.ALWAYS);
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private MfdsBulkMatchingToken() {}

  record Applied(
      long alcoholId,
      String alcoholNameKo,
      String alcoholNameEn,
      Long alcoholDistilleryId,
      Long alcoholRegionId,
      Long distilleryId,
      Long regionId) {}

  static String sign(
      LocalDateTime expiresAt,
      long sourceDeclarationId,
      Applied applied,
      List<MfdsDeclaration> rows,
      java.util.Map<Long, Boolean> adminReleased) {
    List<CanonicalRow> canonicalRows =
        rows.stream()
            .sorted(Comparator.comparing(MfdsDeclaration::getId))
            .map(
                declaration ->
                    row(declaration, Boolean.TRUE.equals(adminReleased.get(declaration.getId()))))
            .toList();
    Canonical canonical =
        new Canonical(
            VERSION,
            expiresAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS).format(DATE_TIME),
            sourceDeclarationId,
            applied.alcoholId(),
            applied.alcoholNameKo(),
            applied.alcoholNameEn(),
            applied.alcoholDistilleryId(),
            applied.alcoholRegionId(),
            applied.distilleryId(),
            applied.regionId(),
            canonicalRows);
    try {
      byte[] json = MAPPER.writeValueAsBytes(canonical);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
    } catch (Exception exception) {
      throw new IllegalStateException("일괄 매칭 미리보기 검증값을 계산하지 못했습니다.", exception);
    }
  }

  static boolean matches(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  private static CanonicalRow row(MfdsDeclaration declaration, boolean adminReleased) {
    MfdsBulkMatchingJudge.Signals signals = MfdsBulkMatchingJudge.signals(declaration);
    return new CanonicalRow(
        declaration.getId(),
        declaration.getRcno(),
        displayName(declaration),
        declaration.getVolumeMl(),
        declaration.getImporterBaseName(),
        declaration.getProcessedDate() == null ? null : declaration.getProcessedDate().format(DATE),
        MfdsBulkMatchingJudge.positive(declaration.getSelectedAlcoholId()),
        MfdsBulkMatchingJudge.positive(declaration.getSelectedDistilleryId()),
        MfdsBulkMatchingJudge.positive(declaration.getSelectedRegionId()),
        signals.age(),
        signals.batch(),
        signals.cask(),
        signals.edition(),
        signals.years(),
        signals.caskStrength(),
        signals.strengthType(),
        signals.abv() == null ? null : signals.abv().toPlainString(),
        signals.versionMarker(),
        signals.variant(),
        signals.country(),
        signals.normalizationReview(),
        signals.genericName(),
        signals.parsedKoAge(),
        signals.parsedEnAge(),
        adminReleased,
        declaration.getProductIdentityKeySha256() == null
            ? null
            : HexFormat.of().formatHex(declaration.getProductIdentityKeySha256()));
  }

  static String displayName(MfdsDeclaration declaration) {
    String name =
        first(
            declaration.getSkuDisplayNameKo(),
            declaration.getBaseProductNameKo(),
            declaration.getAlcoholNameKo(),
            declaration.getNameSearchKeyKo());
    return name == null ? declaration.getRcno() : name;
  }

  private static String first(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private record Canonical(
      String version,
      String expiresAt,
      long sourceDeclarationId,
      long alcoholId,
      String alcoholNameKo,
      String alcoholNameEn,
      Long alcoholDistilleryId,
      Long alcoholRegionId,
      Long distilleryId,
      Long regionId,
      List<CanonicalRow> rows) {}

  private record CanonicalRow(
      Long id,
      String rcno,
      String displayName,
      Integer volumeMl,
      String importerBaseName,
      String processedDate,
      Long currentAlcoholId,
      Long currentDistilleryId,
      Long currentRegionId,
      Integer age,
      String batch,
      String cask,
      String edition,
      List<String> years,
      Boolean caskStrength,
      String strengthType,
      String abv,
      String versionMarker,
      String variant,
      String country,
      boolean normalizationReview,
      boolean genericName,
      Integer parsedKoAge,
      Integer parsedEnAge,
      boolean adminReleased,
      String identityKeyHex) {}
}
