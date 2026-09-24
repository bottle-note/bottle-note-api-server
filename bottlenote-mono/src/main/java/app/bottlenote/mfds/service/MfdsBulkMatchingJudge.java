package app.bottlenote.mfds.service;

import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingReason;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** 같은 제품 키 위의 추가 속성과 현재 연결을 판정한다. 우선순위는 충돌, 변경 불필요, 확인 필요, 적용 가능하다. */
final class MfdsBulkMatchingJudge {

  static final String APPLICABLE = "APPLICABLE";
  static final String NO_CHANGE = "NO_CHANGE";
  static final String NEEDS_REVIEW = "NEEDS_REVIEW";
  static final String CONFLICT = "CONFLICT";
  private static final String GENERIC_REASON = "GENERIC_PRODUCT_NAME_REVIEW_REQUIRED";

  private MfdsBulkMatchingJudge() {}

  record Signals(
      Integer age,
      Integer parsedKoAge,
      Integer parsedEnAge,
      String batch,
      String cask,
      String edition,
      List<String> years,
      Boolean caskStrength,
      String strengthType,
      BigDecimal abv,
      String versionMarker,
      String variant,
      String country,
      boolean normalizationReview,
      boolean genericName) {}

  record Decision(String classification, List<MfdsBulkMatchingReason> reasons) {}

  static Signals signals(MfdsDeclaration declaration) {
    MfdsProductIdentity identity = MfdsProductIdentity.from(declaration);
    String edition = blankToNull(identity.edition());
    if (edition == null) {
      edition = blankToNull(declaration.getEditionName());
    }
    return new Signals(
        declaration.getAgeYears() == null ? null : declaration.getAgeYears().intValue(),
        parsedAge(
            declaration.getSkuDisplayNameKo(),
            declaration.getAlcoholNameKo(),
            declaration.getBaseProductNameKo(),
            declaration.getNameSearchKeyKo()),
        parsedAge(
            declaration.getSkuDisplayNameEn(),
            declaration.getAlcoholNameEn(),
            declaration.getBaseProductNameEn(),
            declaration.getNameSearchKeyEn()),
        blankToNull(identity.batch()),
        blankToNull(identity.cask()),
        edition,
        List.copyOf(identity.years()),
        identity.strength() || variantIndicatesStrength(declaration) ? Boolean.TRUE : null,
        normalizeStrength(declaration.getStrengthType()),
        normalizeAbv(declaration.getAbvPercent()),
        blankToNull(declaration.getVersionMarker()),
        variant(declaration),
        country(declaration),
        declaration.getNormalizationStatus() == MfdsNormalizationStatus.REVIEW_REQUIRED,
        genericName(declaration));
  }

  static Decision decide(
      MfdsDeclaration source,
      MfdsDeclaration target,
      Long alcoholId,
      Long distilleryId,
      Long regionId,
      boolean targetAdminReleased) {
    List<MfdsBulkMatchingReason> reasons = new ArrayList<>();
    reasons.addAll(identityReasons(signals(source), signals(target)));
    if (signals(target).normalizationReview()) {
      reasons.add(
          new MfdsBulkMatchingReason(
              "NORMALIZATION_REVIEW_REQUIRED", "정제 결과가 검토 필요라 자동으로 적용하지 않습니다."));
    }
    if (signals(target).genericName()) {
      reasons.add(
          new MfdsBulkMatchingReason("GENERIC_PRODUCT_NAME", "제품명이 일반명이라 같은 제품으로 보지 않습니다."));
    }
    if (targetAdminReleased) {
      reasons.add(new MfdsBulkMatchingReason("ADMIN_RELEASED", "관리자가 연결을 해제한 신고입니다."));
    }
    LinkDecision links = linkDecision(target, alcoholId, distilleryId, regionId);
    boolean review = !reasons.isEmpty();
    reasons.addAll(links.reasons());
    if (links.conflict()) {
      return new Decision(CONFLICT, List.copyOf(reasons));
    }
    if (!links.fill()) {
      return new Decision(NO_CHANGE, List.copyOf(reasons));
    }
    if (review) {
      return new Decision(NEEDS_REVIEW, List.copyOf(reasons));
    }
    return new Decision(APPLICABLE, List.copyOf(reasons));
  }

  private static List<MfdsBulkMatchingReason> identityReasons(Signals source, Signals target) {
    List<MfdsBulkMatchingReason> reasons = new ArrayList<>();
    Relation storedAge = relate(source.age(), target.age());
    compare(reasons, "AGE", "숙성", storedAge, source.age() == null);
    Integer sourceDisplayAge = displayAge(source);
    Integer targetDisplayAge = displayAge(target);
    Relation displayAge = relate(sourceDisplayAge, targetDisplayAge);
    String storedAgeCode = ageCode(storedAge, source.age() == null);
    String displayAgeCode = ageCode(displayAge, sourceDisplayAge == null);
    if (displayAgeCode != null && !displayAgeCode.equals(storedAgeCode)) {
      compare(reasons, "AGE", "표시명 숙성", displayAge, sourceDisplayAge == null);
    }
    addStoredAgeMismatch(reasons, source, "기준");
    addStoredAgeMismatch(reasons, target, "대상");
    compare(reasons, "BATCH", "배치", relate(source.batch(), target.batch()), source.batch() == null);
    compare(reasons, "CASK", "캐스크", relate(source.cask(), target.cask()), source.cask() == null);
    compare(
        reasons,
        "EDITION",
        "에디션",
        relate(source.edition(), target.edition()),
        source.edition() == null);
    compare(
        reasons, "VINTAGE", "빈티지", relate(source.years(), target.years()), absent(source.years()));
    compare(
        reasons,
        "VERSION",
        "버전",
        relate(source.versionMarker(), target.versionMarker()),
        source.versionMarker() == null);
    compare(
        reasons,
        "VARIANT",
        "변이",
        relate(source.variant(), target.variant()),
        source.variant() == null);
    compare(reasons, "ABV", "도수", relate(source.abv(), target.abv()), source.abv() == null);
    compare(
        reasons,
        "COUNTRY",
        "제조국",
        relate(source.country(), target.country()),
        source.country() == null);
    Relation strengthType = relate(source.strengthType(), target.strengthType());
    Relation caskStrength = relate(source.caskStrength(), target.caskStrength());
    if (strengthType == Relation.MISMATCH
        || strengthType == Relation.ASYMMETRIC
        || caskStrength == Relation.MISMATCH
        || caskStrength == Relation.ASYMMETRIC) {
      reasons.add(new MfdsBulkMatchingReason("STRENGTH_DIFFERS", "도수 유형 또는 캐스크 스트렝스 정보가 다릅니다."));
    }
    return reasons;
  }

  private static void addStoredAgeMismatch(
      List<MfdsBulkMatchingReason> reasons, Signals signals, String side) {
    if (signals.parsedKoAge() != null
        && signals.parsedEnAge() != null
        && !signals.parsedKoAge().equals(signals.parsedEnAge())) {
      reasons.add(
          new MfdsBulkMatchingReason("AGE_TEXT_CONFLICT", side + " 신고의 한글 숙성과 영문 숙성이 서로 다릅니다."));
    }
    Integer parsed = signals.parsedKoAge() != null ? signals.parsedKoAge() : signals.parsedEnAge();
    if (signals.age() != null && parsed != null && !signals.age().equals(parsed)) {
      reasons.add(
          new MfdsBulkMatchingReason(
              "AGE_STORED_MISMATCH", side + " 신고의 저장된 숙성과 표시명 숙성이 서로 다릅니다."));
    }
  }

  private static Integer displayAge(Signals signals) {
    return signals.parsedKoAge() != null ? signals.parsedKoAge() : signals.parsedEnAge();
  }

  private static String ageCode(Relation relation, boolean sourceAbsent) {
    if (relation == Relation.MISMATCH) {
      return "AGE_DIFFERS";
    }
    if (relation == Relation.ASYMMETRIC) {
      return sourceAbsent ? "AGE_MISSING_ON_SOURCE" : "AGE_MISSING_ON_TARGET";
    }
    return null;
  }

  private static Integer parsedAge(String... values) {
    for (String value : values) {
      Integer parsed = MfdsProductIdentity.parsedAge(value);
      if (parsed != null) {
        return parsed;
      }
    }
    return null;
  }

  private static boolean variantIndicatesStrength(MfdsDeclaration declaration) {
    String type = declaration.getVariantMarkerType();
    String raw = declaration.getVariantMarkerRaw();
    if (type != null && type.toUpperCase(Locale.ROOT).contains("STRENGTH")) {
      return true;
    }
    return raw != null && raw.trim().matches("(?i)cs|cask strength");
  }

  private static void compare(
      List<MfdsBulkMatchingReason> reasons,
      String code,
      String label,
      Relation relation,
      boolean sourceAbsent) {
    if (relation == Relation.MISMATCH) {
      reasons.add(new MfdsBulkMatchingReason(code + "_DIFFERS", label + " 정보가 서로 다릅니다."));
    } else if (relation == Relation.ASYMMETRIC) {
      String reasonCode = sourceAbsent ? code + "_MISSING_ON_SOURCE" : code + "_MISSING_ON_TARGET";
      String message =
          sourceAbsent
              ? "기준 신고에는 " + label + " 정보가 없고 대상 신고에만 있습니다."
              : "대상 신고에는 " + label + " 정보가 없고 기준 신고에만 있습니다.";
      reasons.add(new MfdsBulkMatchingReason(reasonCode, message));
    }
  }

  private record LinkDecision(
      boolean conflict, boolean fill, List<MfdsBulkMatchingReason> reasons) {}

  private static LinkDecision linkDecision(
      MfdsDeclaration target, Long alcoholId, Long distilleryId, Long regionId) {
    List<MfdsBulkMatchingReason> reasons = new ArrayList<>();
    boolean conflict = false;
    boolean fill = false;
    boolean selectionDiffers = false;
    boolean referenceDiffers = false;
    boolean referenceClears = false;
    for (Field field :
        List.of(
            field(positive(target.getSelectedAlcoholId()), positive(alcoholId), true),
            field(positive(target.getSelectedDistilleryId()), positive(distilleryId), false),
            field(positive(target.getSelectedRegionId()), positive(regionId), false))) {
      fill = fill || field.fill();
      selectionDiffers = selectionDiffers || field.selectionDiffers();
      referenceDiffers = referenceDiffers || field.referenceDiffers();
      referenceClears = referenceClears || field.referenceClears();
    }
    if (selectionDiffers) {
      reasons.add(
          new MfdsBulkMatchingReason("EXISTING_SELECTION_DIFFERS", "이미 다른 주류가 연결되어 있어 덮어쓰지 않습니다."));
      conflict = true;
    }
    if (referenceDiffers) {
      reasons.add(
          new MfdsBulkMatchingReason(
              "EXISTING_REFERENCE_DIFFERS", "이미 다른 증류소 또는 지역이 연결되어 있어 덮어쓰지 않습니다."));
      conflict = true;
    }
    if (referenceClears) {
      reasons.add(
          new MfdsBulkMatchingReason(
              "EXISTING_REFERENCE_WOULD_CLEAR", "이미 연결된 증류소 또는 지역을 비우는 변경은 적용하지 않습니다."));
      conflict = true;
    }
    return new LinkDecision(conflict, fill, reasons);
  }

  private record Field(
      boolean fill, boolean selectionDiffers, boolean referenceDiffers, boolean referenceClears) {}

  private static Field field(Long current, Long applied, boolean alcohol) {
    if (Objects.equals(current, applied)) {
      return new Field(false, false, false, false);
    }
    if (current == null) {
      return new Field(true, false, false, false);
    }
    if (applied == null) {
      return new Field(false, alcohol, !alcohol, !alcohol);
    }
    return new Field(false, alcohol, !alcohol, false);
  }

  static Long positive(Long id) {
    return id != null && id > 0 ? id : null;
  }

  private enum Relation {
    SAME,
    BOTH_ABSENT,
    MISMATCH,
    ASYMMETRIC
  }

  private static Relation relate(Object left, Object right) {
    boolean leftAbsent = absent(left);
    boolean rightAbsent = absent(right);
    if (leftAbsent && rightAbsent) {
      return Relation.BOTH_ABSENT;
    }
    if (leftAbsent || rightAbsent) {
      return Relation.ASYMMETRIC;
    }
    if (left instanceof BigDecimal leftNumber && right instanceof BigDecimal rightNumber) {
      return leftNumber.compareTo(rightNumber) == 0 ? Relation.SAME : Relation.MISMATCH;
    }
    return Objects.equals(left, right) ? Relation.SAME : Relation.MISMATCH;
  }

  private static boolean absent(Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof String text) {
      return text.isBlank();
    }
    if (value instanceof List<?> list) {
      return list.isEmpty();
    }
    return false;
  }

  private static String variant(MfdsDeclaration declaration) {
    String type = blankToNull(declaration.getVariantMarkerType());
    String raw = blankToNull(declaration.getVariantMarkerRaw());
    String value = blankToNull(declaration.getVariantMarkerValue());
    if (type == null && raw == null && value == null) {
      return null;
    }
    return (type == null ? "" : type)
        + "|"
        + (raw == null ? "" : raw)
        + "|"
        + (value == null ? "" : value);
  }

  private static String country(MfdsDeclaration declaration) {
    String value = blankToNull(declaration.getManufactureCountryAlpha2());
    return value == null ? null : value.toUpperCase(Locale.ROOT);
  }

  private static boolean genericName(MfdsDeclaration declaration) {
    List<String> reasons = declaration.getNormalizationReasons();
    return reasons != null && reasons.contains(GENERIC_REASON);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String normalizeStrength(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }

  private static BigDecimal normalizeAbv(BigDecimal value) {
    return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
  }
}
