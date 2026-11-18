package app.bottlenote.mfds.service;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 원문에서 제품 식별 정보를 추출하며 검색 키는 원문이 없을 때만 사용한다. */
record MfdsProductIdentity(
    String ko,
    String en,
    String brandKo,
    String brandEn,
    Integer age,
    Set<String> years,
    String batch,
    String cask,
    String edition,
    boolean strength,
    Set<String> words,
    String category) {
  private static final Pattern AGE =
      Pattern.compile(
          "(?<![0-9])([1-9][0-9]?)\\s*(?:years?\\s*old|years?|yrs?|yo|y|년)(?![a-z0-9])");
  private static final Pattern EDITION = Pattern.compile("(?:edition|에디션)\\s*(?:no\\s*)?([0-9]+)");
  private static final Pattern YEAR = Pattern.compile("(?<![0-9])(?:19|20)[0-9]{2}(?![0-9])");
  private static final Pattern BATCH =
      Pattern.compile("(?:batch|배치|배취)\\s*(?:no\\s*)?([a-z]?[0-9]+[a-z]?)");
  private static final Pattern CASK = Pattern.compile("(?:cask|캐스크|캐스크번호)\\s*(?:no\\s*)?([0-9]+)");
  private static final Set<String> GENERIC =
      Set.of(
          "the",
          "single",
          "malt",
          "scotch",
          "sctoch",
          "whisky",
          "whiskey",
          "japanese",
          "japan",
          "highland",
          "highlands",
          "speyside",
          "islay",
          "irish",
          "kentucky",
          "straight",
          "aged",
          "years",
          "year",
          "yrs",
          "yr",
          "old",
          "yo",
          "y",
          "cask",
          "strength",
          "cs",
          "batch",
          "no",
          "edition",
          "limited",
          "release",
          "bottled",
          "bottling",
          "distillery",
          "finish",
          "finished",
          "wood",
          "위스키",
          "싱글",
          "몰트");
  private static final Set<String> MULTI_PREFIX =
      Set.of(
          "glen",
          "ben",
          "port",
          "old",
          "jim",
          "jack",
          "johnnie",
          "wild",
          "woodford",
          "makers",
          "maker",
          "pappy",
          "van",
          "royal",
          "black",
          "white",
          "highland",
          "loch",
          "chivas",
          "buffalo");

  static MfdsProductIdentity from(MfdsDeclaration d) {
    String ko =
        normalize(
            first(
                d.getSkuDisplayNameKo(),
                d.getAlcoholNameKo(),
                d.getBaseProductNameKo(),
                d.getNameSearchKeyKo()));
    String en =
        normalize(
            first(
                d.getSkuDisplayNameEn(),
                d.getAlcoholNameEn(),
                d.getBaseProductNameEn(),
                d.getNameSearchKeyEn()));
    String text = ko + " " + en;
    Integer parsedAge = age(text);
    Integer age =
        parsedAge != null ? parsedAge : d.getAgeYears() == null ? null : d.getAgeYears().intValue();
    Set<String> years = years(text);
    if (d.getVintageYear() != null) years.add(d.getVintageYear().toString());
    return create(
        ko,
        en,
        "",
        "",
        age,
        years,
        first(d.getBatchNumber(), extract(BATCH, text)),
        first(d.getCaskNumber(), extract(CASK, text)),
        text + " " + normalize(d.getEditionName()),
        category(d.getAlcoholCategoryKo(), d.getAlcoholCategoryEn()));
  }

  static MfdsProductIdentity from(AlcoholMatchTargetItem t) {
    String ko = normalize(t.korName()), en = normalize(t.engName());
    String text = ko + " " + en;
    Integer age = number(t.age());
    return create(
        ko,
        en,
        normalize(t.korDistillery()),
        normalize(t.engDistillery()),
        age != null ? age : age(text),
        years(text),
        extract(BATCH, text),
        extract(CASK, text),
        text,
        category(t.korCategory(), t.engCategory()));
  }

  private static MfdsProductIdentity create(
      String ko,
      String en,
      String distilleryKo,
      String distilleryEn,
      Integer age,
      Set<String> years,
      String batch,
      String cask,
      String text,
      String category) {
    String brandEn = brand(en, distilleryEn), brandKo = brand(ko, distilleryKo);
    String residual = en.isBlank() ? ko : en;
    String brand = en.isBlank() ? brandKo : brandEn;
    if (!brand.isBlank()) residual = residual.replaceFirst(Pattern.quote(brand), " ");
    residual = AGE.matcher(residual).replaceAll(" ");
    residual = YEAR.matcher(residual).replaceAll(" ");
    residual = BATCH.matcher(residual).replaceAll(" ");
    residual = CASK.matcher(residual).replaceAll(" ");
    residual = EDITION.matcher(residual).replaceAll(" ");
    Set<String> words =
        Arrays.stream(residual.split("\\s+"))
            .filter(w -> !w.isBlank() && !GENERIC.contains(w) && !w.matches("[0-9]+"))
            .collect(Collectors.toCollection(TreeSet::new));
    boolean strength =
        Pattern.compile("(?:\\bcs\\b|cask strength|캐스크 스트렝스|캐스크스트렝스|캐스크 스트렝쓰)")
            .matcher(text)
            .find();
    return new MfdsProductIdentity(
        ko,
        en,
        brandKo,
        brandEn,
        age,
        Collections.unmodifiableSet(new TreeSet<>(years)),
        identifier(batch),
        identifier(cask),
        identifier(extract(EDITION, text)),
        strength,
        Collections.unmodifiableSet(new TreeSet<>(words)),
        category);
  }

  private static String brand(String name, String distillery) {
    if (name.isBlank()) return "";
    String stripped = name.replaceFirst("^the\\s+", "");
    if (distillery.length() >= 3 && stripped.startsWith(distillery + " ")) return distillery;
    String[] parts = stripped.split("\\s+");
    String first = parts[0].replaceFirst("[0-9].*$", "");
    if (MULTI_PREFIX.contains(first) && parts.length > 1 && !parts[1].matches(".*[0-9].*")) {
      return first
          + " "
          + parts[1]
          + (first.equals("pappy") && parts.length > 2 ? " " + parts[2] : "");
    }
    return GENERIC.contains(first) || first.length() < 2 ? "" : first;
  }

  static String normalize(String value) {
    if (value == null) return "";
    return Normalizer.normalize(value, Normalizer.Form.NFKC)
        .toLowerCase(Locale.ROOT)
        .replaceAll("(?<![a-z0-9])[0-9]+(?:[.,][0-9]+)?\\s*(?:ml|cl|l|밀리리터|리터)(?![a-z])", " ")
        .replaceAll("double\\s+wood", "doublewood")
        .replaceAll("port\\s+wood", "portwood")
        .replace("’", "")
        .replace("'", "")
        .replaceAll("[^0-9a-z가-힣]+", " ")
        .trim()
        .replaceAll("\\s+", " ");
  }

  static String compact(String value) {
    return normalize(value).replace(" ", "");
  }

  private static String identifier(String value) {
    String v = compact(value).replaceFirst("^(?:batch|배치|배취|cask|캐스크)(?:no)?", "");
    return v.replaceFirst("^0+(?!$)", "");
  }

  /** 정규화한 표시명에서 숙성 연수를 읽는다. 저장 연수와 섞지 않는다. */
  static Integer parsedAge(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return age(normalize(value));
  }

  private static Integer age(String text) {
    String value = extract(AGE, text);
    return value == null ? null : Integer.valueOf(value);
  }

  private static Integer number(String text) {
    if (text == null || !text.trim().matches("[1-9][0-9]?(?:\\.0)?")) return null;
    return (int) Double.parseDouble(text.trim());
  }

  private static Set<String> years(String text) {
    Set<String> result = new TreeSet<>();
    Matcher m = YEAR.matcher(text);
    while (m.find()) result.add(m.group());
    return result;
  }

  private static String extract(Pattern p, String text) {
    Matcher m = p.matcher(text);
    return m.find() ? m.group(1) : null;
  }

  private static String first(String... values) {
    return Arrays.stream(values).filter(v -> v != null && !v.isBlank()).findFirst().orElse("");
  }

  private static String category(String ko, String en) {
    String s = normalize(ko) + " " + normalize(en);
    if (s.matches(".*(whisk[ey]*|위스키|single malt|blended|bourbon|rye|싱글 몰트|블렌디드|버번).*"))
      return "WHISKY";
    if (s.matches(".*(brandy|cognac|브랜디).*")) return "BRANDY";
    if (s.matches(".*(liqueur|리큐르).*")) return "LIQUEUR";
    if (s.matches(".*(rum|럼).*")) return "RUM";
    return "";
  }
}
