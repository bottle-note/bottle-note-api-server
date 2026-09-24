package app.bottlenote.mfds.service;

import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingReasonItem;
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

  record Decision(String classification, List<MfdsBulkMatchingReasonItem> reasons) {}

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
        upper(declaration.getStrengthType()),
        normalizeAbv(declaration.getAbvPercent()),
        blankToNull(declaration.getVersionMarker()),
        variant(declaration),
        upper(declaration.getManufactureCountryAlpha2()),
        declaration.getNormalizationStatus() == MfdsNormalizationStatus.REVIEW_REQUIRED,
        genericName(declaration));
  }

  /** 신호는 호출자가 행마다 한 번 계산해 넘긴다. */
  static Decision decide(
      Signals source,
      Signals target,
      MfdsDeclaration targetRow,
      MfdsMatchingService.MatchTarget applied,
      boolean targetAdminReleased) {
    List<MfdsBulkMatchingReasonItem> reasons = new ArrayList<>();
    reasons.addAll(identityReasons(source, target));
    if (target.normalizationReview()) {
      reasons.add(reason("NORMALIZATION_REVIEW_REQUIRED", "정제 결과가 검토 필요라 자동으로 적용하지 않습니다."));
    }
    if (target.genericName()) {
      reasons.add(reason("GENERIC_PRODUCT_NAME", "제품명이 일반명이라 같은 제품으로 보지 않습니다."));
    }
    if (targetAdminReleased) {
      reasons.add(reason("ADMIN_RELEASED", "관리자가 연결을 해제한 신고입니다."));
    }
    boolean review = !reasons.isEmpty();
    Long currentAlcohol = positive(targetRow.getSelectedAlcoholId());
    Long alcohol = positive(applied.alcohol().alcoholId());
    List<Long[]> references =
        List.of(
            new Long[] {
              positive(targetRow.getSelectedDistilleryId()), positive(applied.distilleryId())
            },
            new Long[] {positive(targetRow.getSelectedRegionId()), positive(applied.regionId())});
    boolean fill =
        currentAlcohol == null && alcohol != null
            || references.stream().anyMatch(ids -> ids[0] == null && ids[1] != null);
    List<MfdsBulkMatchingReasonItem> conflicts = new ArrayList<>();
    if (currentAlcohol != null && !currentAlcohol.equals(alcohol)) {
      conflicts.add(reason("EXISTING_SELECTION_DIFFERS", "이미 다른 주류가 연결되어 있어 덮어쓰지 않습니다."));
    }
    if (references.stream().anyMatch(ids -> ids[0] != null && !ids[0].equals(ids[1]))) {
      conflicts.add(reason("EXISTING_REFERENCE_DIFFERS", "이미 다른 증류소 또는 지역이 연결되어 있어 덮어쓰지 않습니다."));
    }
    if (references.stream().anyMatch(ids -> ids[0] != null && ids[1] == null)) {
      conflicts.add(
          reason("EXISTING_REFERENCE_WOULD_CLEAR", "이미 연결된 증류소 또는 지역을 비우는 변경은 적용하지 않습니다."));
    }
    reasons.addAll(conflicts);
    String classification =
        !conflicts.isEmpty() ? CONFLICT : !fill ? NO_CHANGE : review ? NEEDS_REVIEW : APPLICABLE;
    return new Decision(classification, List.copyOf(reasons));
  }

  private static MfdsBulkMatchingReasonItem reason(String code, String message) {
    return new MfdsBulkMatchingReasonItem(code, message);
  }

  private static List<MfdsBulkMatchingReasonItem> identityReasons(Signals source, Signals target) {
    List<MfdsBulkMatchingReasonItem> reasons = new ArrayList<>();
    Relation storedAge = compare(reasons, "AGE", "숙성", source.age(), target.age());
    Integer sourceDisplayAge = displayAge(source);
    Integer targetDisplayAge = displayAge(target);
    String storedAgeCode = ageCode(storedAge, source.age() == null);
    String displayAgeCode =
        ageCode(relate(sourceDisplayAge, targetDisplayAge), sourceDisplayAge == null);
    if (displayAgeCode != null && !displayAgeCode.equals(storedAgeCode)) {
      compare(reasons, "AGE", "표시명 숙성", sourceDisplayAge, targetDisplayAge);
    }
    addStoredAgeMismatch(reasons, source, "기준");
    addStoredAgeMismatch(reasons, target, "대상");
    compare(reasons, "BATCH", "배치", source.batch(), target.batch());
    compare(reasons, "CASK", "캐스크", source.cask(), target.cask());
    compare(reasons, "EDITION", "에디션", source.edition(), target.edition());
    compare(reasons, "VINTAGE", "빈티지", source.years(), target.years());
    compare(reasons, "VERSION", "버전", source.versionMarker(), target.versionMarker());
    compare(reasons, "VARIANT", "변이", source.variant(), target.variant());
    compare(reasons, "ABV", "도수", source.abv(), target.abv());
    compare(reasons, "COUNTRY", "제조국", source.country(), target.country());
    if (differs(relate(source.strengthType(), target.strengthType()))
        || differs(relate(source.caskStrength(), target.caskStrength()))) {
      reasons.add(reason("STRENGTH_DIFFERS", "도수 유형 또는 캐스크 스트렝스 정보가 다릅니다."));
    }
    return reasons;
  }

  private static void addStoredAgeMismatch(
      List<MfdsBulkMatchingReasonItem> reasons, Signals signals, String side) {
    if (signals.parsedKoAge() != null
        && signals.parsedEnAge() != null
        && !signals.parsedKoAge().equals(signals.parsedEnAge())) {
      reasons.add(reason("AGE_TEXT_CONFLICT", side + " 신고의 한글 숙성과 영문 숙성이 서로 다릅니다."));
    }
    Integer parsed = displayAge(signals);
    if (signals.age() != null && parsed != null && !signals.age().equals(parsed)) {
      reasons.add(reason("AGE_STORED_MISMATCH", side + " 신고의 저장된 숙성과 표시명 숙성이 서로 다릅니다."));
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

  private static Relation compare(
      List<MfdsBulkMatchingReasonItem> reasons,
      String code,
      String label,
      Object source,
      Object target) {
    Relation relation = relate(source, target);
    boolean sourceAbsent = absent(source);
    if (relation == Relation.MISMATCH) {
      reasons.add(reason(code + "_DIFFERS", label + " 정보가 서로 다릅니다."));
    } else if (relation == Relation.ASYMMETRIC) {
      String reasonCode = sourceAbsent ? code + "_MISSING_ON_SOURCE" : code + "_MISSING_ON_TARGET";
      String message =
          sourceAbsent
              ? "기준 신고에는 " + label + " 정보가 없고 대상 신고에만 있습니다."
              : "대상 신고에는 " + label + " 정보가 없고 기준 신고에만 있습니다.";
      reasons.add(reason(reasonCode, message));
    }
    return relation;
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

  private static boolean differs(Relation relation) {
    return relation == Relation.MISMATCH || relation == Relation.ASYMMETRIC;
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

  private static boolean genericName(MfdsDeclaration declaration) {
    List<String> reasons = declaration.getNormalizationReasons();
    return reasons != null && reasons.contains(GENERIC_REASON);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String upper(String value) {
    String trimmed = blankToNull(value);
    return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
  }

  private static BigDecimal normalizeAbv(BigDecimal value) {
    return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
  }
}
