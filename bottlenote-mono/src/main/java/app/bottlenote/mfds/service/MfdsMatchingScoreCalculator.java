package app.bottlenote.mfds.service;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetailItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** MFDS 제품의 브랜드와 속성 일치 여부를 점수화한다. */
@Component
public class MfdsMatchingScoreCalculator {

  private static final double ABV_TOLERANCE = 10.0;
  private static final int SCORE_SCALE = 4;

  private static final Pattern NON_ALLOWED_CHARS = Pattern.compile("[^0-9a-z가-힣]+");
  private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

  /** 브랜드와 식별 속성을 먼저 비교하고 불일치에 상한을 적용한다. */
  public MfdsMatchScoreDetailItem scoreAlcohol(
      MfdsDeclaration declaration, AlcoholMatchTargetItem target) {
    var source = MfdsProductIdentity.from(declaration);
    var candidate = MfdsProductIdentity.from(target);
    var reasons = new ArrayList<MfdsMatchScoreDetailItem.AttributeComparison>();
    Double brand = brandSimilarity(source, candidate);
    compare(
        reasons,
        "BRAND",
        source.brandEn().isBlank() ? source.brandKo() : source.brandEn(),
        candidate.brandEn().isBlank() ? candidate.brandKo() : candidate.brandEn(),
        brand == null ? "UNKNOWN" : brand >= 0.85 ? "MATCH" : "MISMATCH");
    Double name = productSimilarity(source, candidate);
    compare(
        reasons,
        "PRODUCT",
        String.join(" ", source.words()),
        String.join(" ", candidate.words()),
        name == null ? "UNKNOWN" : name >= 0.8 ? "MATCH" : "MISMATCH");
    compareValue(reasons, "AGE", source.age(), candidate.age());
    compareValue(reasons, "BATCH", source.batch(), candidate.batch());
    compareValue(reasons, "CASK", source.cask(), candidate.cask());
    compareValue(reasons, "EDITION", source.edition(), candidate.edition());
    compare(
        reasons,
        "YEAR",
        source.years().toString(),
        candidate.years().toString(),
        source.years().isEmpty() || candidate.years().isEmpty()
            ? "UNKNOWN"
            : source.years().equals(candidate.years()) ? "MATCH" : "MISMATCH");
    compare(
        reasons,
        "CASK_STRENGTH",
        source.strength() ? "CS" : null,
        candidate.strength() ? "CS" : null,
        source.strength() && candidate.strength() ? "MATCH" : "UNKNOWN");
    compareValue(reasons, "CATEGORY", source.category(), candidate.category());

    Double abv = abvProximity(declaration.getAbvPercent(), target.abv());
    Double age =
        source.age() == null || candidate.age() == null
            ? null
            : source.age().equals(candidate.age()) ? 1.0 : 0.0;
    Double category =
        source.category().isEmpty() || candidate.category().isEmpty()
            ? null
            : source.category().equals(candidate.category()) ? 1.0 : 0.0;
    Double region = alcoholRegionSimilarity(declaration, target);
    double sum = 0, weights = 0;
    Double[] values = {brand, name, age, abv, category};
    double[] factors = {0.4, 0.35, 0.15, 0.07, 0.03};
    for (int i = 0; i < values.length; i++) {
      if (values[i] != null) {
        sum += values[i] * factors[i];
        weights += factors[i];
      }
    }
    double score = weights == 0 ? 0 : sum / weights;
    boolean review = false;
    if (source.ko().isBlank() && source.en().isBlank()) score = 0;
    if (brand != null && brand < 0.85) score = Math.min(score, 0.25);
    if (brand == null) {
      score = Math.min(score, name != null && name >= 0.8 ? 0.59 : 0.25);
      review = true;
    }
    if (name == null) {
      score = Math.min(score, source.words().isEmpty() ? 0.79 : 0.59);
      review = true;
    } else if (name < 0.25) score = Math.min(score, 0.25);
    else if (name < 0.8) {
      score *= 0.8;
      review = true;
    }
    for (var reason : reasons) {
      if (Set.of("AGE", "BATCH", "CASK", "YEAR", "EDITION", "CATEGORY").contains(reason.attribute())
          && reason.status().equals("MISMATCH")) score = Math.min(score, 0.2);
      if (Set.of("AGE", "BATCH", "CASK", "YEAR", "EDITION").contains(reason.attribute())
          && reason.status().equals("UNKNOWN")
          && !missing(reason.sourceValue())) {
        score *= 0.85;
        review = true;
      }
    }
    if (reasons.stream()
        .anyMatch(
            r ->
                Set.of("AGE", "BATCH", "CASK", "YEAR", "EDITION").contains(r.attribute())
                    && r.status().equals("UNKNOWN")
                    && (!missing(r.sourceValue()) || !missing(r.targetValue())))) review = true;
    if (source.strength() != candidate.strength()) {
      score *= 0.65;
      review = true;
    }
    if (abv != null && abv < 0.5) {
      score *= 0.8;
      review = true;
    }
    if (source.age() != null
        && declaration.getAgeYears() != null
        && source.age().intValue() != declaration.getAgeYears().intValue()) {
      compare(
          reasons,
          "SOURCE_AGE",
          declaration.getAgeYears().toString(),
          source.age().toString(),
          "MISMATCH");
      review = true;
    }
    return new MfdsMatchScoreDetailItem(
        toBigDecimal(name),
        toBigDecimal(abv),
        toBigDecimal(age),
        toBigDecimal(category),
        toBigDecimal(region),
        toBigDecimal(score),
        toBigDecimal(brand),
        review || score < 0.8,
        List.copyOf(reasons));
  }

  private static boolean missing(String value) {
    return value == null || value.isBlank() || value.equals("[]");
  }

  private static void compareValue(
      List<MfdsMatchScoreDetailItem.AttributeComparison> reasons,
      String attribute,
      Object source,
      Object target) {
    String left = source == null ? null : source.toString();
    String right = target == null ? null : target.toString();
    compare(
        reasons,
        attribute,
        left,
        right,
        missing(left) || missing(right) ? "UNKNOWN" : left.equals(right) ? "MATCH" : "MISMATCH");
  }

  private static void compare(
      List<MfdsMatchScoreDetailItem.AttributeComparison> reasons,
      String attribute,
      String source,
      String target,
      String status) {
    reasons.add(
        new MfdsMatchScoreDetailItem.AttributeComparison(attribute, source, target, status));
  }

  private Double brandSimilarity(MfdsProductIdentity source, MfdsProductIdentity target) {
    Double en =
        textSimilarity(
            MfdsProductIdentity.compact(source.brandEn()),
            MfdsProductIdentity.compact(target.brandEn()));
    Double ko =
        textSimilarity(
            MfdsProductIdentity.compact(source.brandKo()),
            MfdsProductIdentity.compact(target.brandKo()));
    return maxSimilarity(en, ko);
  }

  private Double productSimilarity(MfdsProductIdentity source, MfdsProductIdentity target) {
    if (source.words().isEmpty() || target.words().isEmpty()) return null;
    return jaccard(source.words(), target.words());
  }

  /** 증류소 후보 점수. 신고 데이터에 증류소 이름 후보가 없으면 null을 반환한다. */
  public BigDecimal scoreDistillery(MfdsDeclaration declaration, DistilleryMatchTargetItem target) {
    Double similarity =
        maxSimilarity(
            textSimilarity(declaration.getDistilleryNameKoCandidate(), target.korName()),
            textSimilarity(declaration.getDistilleryNameEnCandidate(), target.engName()));
    return toBigDecimal(similarity);
  }

  /** 지역 후보 점수. 신고 데이터에 지역·제조국 정보가 없으면 null을 반환한다. */
  public BigDecimal scoreRegion(MfdsDeclaration declaration, RegionMatchTargetItem target) {
    Double similarity =
        maxSimilarity(
            textSimilarity(declaration.getAlcoholRegionKo(), target.korName()),
            textSimilarity(declaration.getManufactureCountryNameKo(), target.korName()),
            textSimilarity(declaration.getAlcoholRegionEn(), target.engName()),
            textSimilarity(declaration.getManufactureCountryNameEn(), target.engName()));
    return toBigDecimal(similarity);
  }

  private Double abvProximity(BigDecimal declarationAbv, String targetAbv) {
    Double parsedTarget = parseFirstNumber(targetAbv);
    if (declarationAbv == null || parsedTarget == null) {
      return null;
    }
    double diff = Math.abs(declarationAbv.doubleValue() - parsedTarget);
    return Math.max(0.0, 1.0 - diff / ABV_TOLERANCE);
  }

  private Double alcoholRegionSimilarity(
      MfdsDeclaration declaration, AlcoholMatchTargetItem target) {
    return maxSimilarity(
        textSimilarity(declaration.getAlcoholRegionKo(), target.korRegion()),
        textSimilarity(declaration.getManufactureCountryNameKo(), target.korRegion()),
        textSimilarity(declaration.getAlcoholRegionEn(), target.engRegion()),
        textSimilarity(declaration.getManufactureCountryNameEn(), target.engRegion()));
  }

  /** 정규화 후 토큰 자카드 유사도와 편집거리 비율 중 큰 값. 어느 한쪽이 비어 있으면 null. */
  private Double textSimilarity(String left, String right) {
    String normalizedLeft = normalize(left);
    String normalizedRight = normalize(right);
    if (normalizedLeft.isEmpty() || normalizedRight.isEmpty()) {
      return null;
    }
    double jaccard = jaccard(tokens(normalizedLeft), tokens(normalizedRight));
    double levenshtein = levenshteinRatio(normalizedLeft, normalizedRight);
    return Math.max(jaccard, levenshtein);
  }

  private static Double maxSimilarity(Double... similarities) {
    return Arrays.stream(similarities).filter(Objects::nonNull).max(Double::compareTo).orElse(null);
  }

  private static String normalize(String value) {
    if (value == null) {
      return "";
    }
    String lowered = value.toLowerCase(Locale.ROOT);
    Matcher matcher = NON_ALLOWED_CHARS.matcher(lowered);
    return matcher.replaceAll(" ").trim();
  }

  private static Set<String> tokens(String value) {
    String normalized = normalize(value);
    if (normalized.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(Arrays.asList(normalized.split("\\s+")));
  }

  private static double jaccard(Set<String> left, Set<String> right) {
    if (left.isEmpty() || right.isEmpty()) {
      return 0.0;
    }
    Set<String> intersection = new HashSet<>(left);
    intersection.retainAll(right);
    Set<String> union = new HashSet<>(left);
    union.addAll(right);
    return (double) intersection.size() / union.size();
  }

  private static double levenshteinRatio(String left, String right) {
    int maxLength = Math.max(left.length(), right.length());
    if (maxLength == 0) {
      return 0.0;
    }
    return 1.0 - (double) levenshteinDistance(left, right) / maxLength;
  }

  private static int levenshteinDistance(String left, String right) {
    int[] previous = new int[right.length() + 1];
    int[] current = new int[right.length() + 1];
    for (int j = 0; j <= right.length(); j++) {
      previous[j] = j;
    }
    for (int i = 1; i <= left.length(); i++) {
      current[0] = i;
      for (int j = 1; j <= right.length(); j++) {
        int substitutionCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
        current[j] =
            Math.min(
                Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + substitutionCost);
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[right.length()];
  }

  private static Double parseFirstNumber(String value) {
    if (value == null) {
      return null;
    }
    Matcher matcher = FIRST_NUMBER.matcher(value);
    if (!matcher.find()) {
      return null;
    }
    return Double.parseDouble(matcher.group(1));
  }

  private static BigDecimal toBigDecimal(Double value) {
    if (value == null) {
      return null;
    }
    return BigDecimal.valueOf(value).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
  }
}
