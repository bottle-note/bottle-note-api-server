package app.bottlenote.mfds.service;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetail;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * MFDS 정제 수입 원장과 BottleNote 원본 데이터의 유사도를 점수화한다.
 *
 * <p>점수 산식 v1: 이름 0.5, 카테고리 0.15, 도수 0.15, 숙성/빈티지 0.1, 지역 0.1 가중 합산. 비교 데이터가 없는 요소는 가중치에서 제외하고 남은
 * 가중치로 재정규화한다. 이름 비교가 불가능하면 전체 점수는 0이다.
 */
@Component
public class MfdsMatchingScoreCalculator {

  private static final double NAME_WEIGHT = 0.5;
  private static final double CATEGORY_WEIGHT = 0.15;
  private static final double ABV_WEIGHT = 0.15;
  private static final double AGE_WEIGHT = 0.1;
  private static final double REGION_WEIGHT = 0.1;

  private static final double ABV_TOLERANCE = 10.0;
  private static final int SCORE_SCALE = 6;

  private static final Pattern NON_ALLOWED_CHARS = Pattern.compile("[^0-9a-z가-힣]+");
  private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

  /** 알코올 후보 점수와 요소별 근거를 계산한다. */
  public MfdsMatchScoreDetail scoreAlcohol(
      MfdsDeclaration declaration, AlcoholMatchTargetItem target) {
    Double nameScore = alcoholNameSimilarity(declaration, target);
    if (nameScore == null) {
      return new MfdsMatchScoreDetail(null, null, null, null, null, toBigDecimal(0.0));
    }

    Double abvScore = abvProximity(declaration.getAbvPercent(), target.abv());
    Double ageScore = ageMatch(declaration, target);
    Double categoryScore = categorySimilarity(declaration, target);
    Double regionScore = alcoholRegionSimilarity(declaration, target);

    double weightedSum = NAME_WEIGHT * nameScore;
    double weightSum = NAME_WEIGHT;
    if (abvScore != null) {
      weightedSum += ABV_WEIGHT * abvScore;
      weightSum += ABV_WEIGHT;
    }
    if (ageScore != null) {
      weightedSum += AGE_WEIGHT * ageScore;
      weightSum += AGE_WEIGHT;
    }
    if (categoryScore != null) {
      weightedSum += CATEGORY_WEIGHT * categoryScore;
      weightSum += CATEGORY_WEIGHT;
    }
    if (regionScore != null) {
      weightedSum += REGION_WEIGHT * regionScore;
      weightSum += REGION_WEIGHT;
    }

    return new MfdsMatchScoreDetail(
        toBigDecimal(nameScore),
        toBigDecimal(abvScore),
        toBigDecimal(ageScore),
        toBigDecimal(categoryScore),
        toBigDecimal(regionScore),
        toBigDecimal(weightedSum / weightSum));
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

  private Double alcoholNameSimilarity(MfdsDeclaration declaration, AlcoholMatchTargetItem target) {
    String koName =
        firstNonBlank(
            declaration.getNameSearchKeyKo(),
            declaration.getAlcoholNameKo(),
            declaration.getBaseProductNameKo());
    String enName =
        firstNonBlank(
            declaration.getNameSearchKeyEn(),
            declaration.getAlcoholNameEn(),
            declaration.getBaseProductNameEn());
    return maxSimilarity(
        textSimilarity(koName, target.korName()), textSimilarity(enName, target.engName()));
  }

  private Double abvProximity(BigDecimal declarationAbv, String targetAbv) {
    Double parsedTarget = parseFirstNumber(targetAbv);
    if (declarationAbv == null || parsedTarget == null) {
      return null;
    }
    double diff = Math.abs(declarationAbv.doubleValue() - parsedTarget);
    return Math.max(0.0, 1.0 - diff / ABV_TOLERANCE);
  }

  /**
   * 숙성 연수는 양쪽 값이 있으면 일치 여부(±1년까지 부분 점수)로 본다. 신고에 숙성 연수가 없고 빈티지 연도만 있으면 알코올 이름에 그 연도가 등장하는지로 대체하며,
   * 등장하지 않는 경우는 판단 불가로 제외한다.
   */
  private Double ageMatch(MfdsDeclaration declaration, AlcoholMatchTargetItem target) {
    Double targetAge = parseFirstNumber(target.age());
    if (declaration.getAgeYears() != null && targetAge != null) {
      double diff = Math.abs(declaration.getAgeYears() - targetAge);
      if (diff == 0) {
        return 1.0;
      }
      return diff <= 1 ? 0.5 : 0.0;
    }
    if (declaration.getVintageYear() != null) {
      String vintage = String.valueOf(declaration.getVintageYear());
      if (tokens(target.korName()).contains(vintage)
          || tokens(target.engName()).contains(vintage)) {
        return 1.0;
      }
    }
    return null;
  }

  private Double categorySimilarity(MfdsDeclaration declaration, AlcoholMatchTargetItem target) {
    return maxSimilarity(
        textSimilarity(declaration.getAlcoholCategoryKo(), target.korCategory()),
        textSimilarity(declaration.getAlcoholCategoryEn(), target.engCategory()));
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

  private static String firstNonBlank(String... values) {
    return Arrays.stream(values)
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private static BigDecimal toBigDecimal(Double value) {
    if (value == null) {
      return null;
    }
    return BigDecimal.valueOf(value).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
  }
}
