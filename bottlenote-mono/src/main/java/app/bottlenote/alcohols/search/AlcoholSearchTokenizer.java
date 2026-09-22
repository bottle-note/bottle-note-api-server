package app.bottlenote.alcohols.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 주류 검색 입력({@code keyword}, {@code keywords}, query string)을 공통 토큰 목록으로 정규화한다.
 *
 * <p>공백 분리에 더해 한글/영문/숫자 경계와 검색 유의 기호를 처리한다. 유니코드 분해(NFD)나 n-gram은 사용하지 않는다.
 *
 * <p>기호는 분리만 하고 문자를 바꾸지 않는다. 아포스트로피 접합이나 {@code &}→{@code and} 같은 변환은 정규화된 문서끼리 비교하는 snapshot 경로에서만
 * 안전하고, 원문 컬럼에 LIKE를 거는 explore/admin 경로에서는 {@code Maker's Mark}처럼 기존에 매칭되던 이름을 놓치게 만든다.
 */
public final class AlcoholSearchTokenizer {

  private enum CharClass {
    HANGUL,
    LATIN,
    DIGIT,
    OTHER
  }

  private AlcoholSearchTokenizer() {}

  public static List<String> tokenize(String input) {
    if (input == null || input.isBlank()) {
      return List.of();
    }

    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    CharClass currentClass = null;

    String normalized = input.trim().toLowerCase(Locale.ROOT);
    for (int offset = 0; offset < normalized.length(); ) {
      int codePoint = normalized.codePointAt(offset);
      offset += Character.charCount(codePoint);

      if (isIgnorable(codePoint)) {
        continue;
      }

      if (isWhitespace(codePoint) || isSeparator(codePoint)) {
        // `D.E.W.`, `W&Y`처럼 한 글자씩 기호로 이어진 약어는 쪼개면 한 글자 토큰만 남아 전체 목록에 매칭되므로 원문 그대로 둔다.
        if (isAbbreviationJoiner(codePoint)
            && isSingleLetterAbbreviation(current)
            && nextIsSingleLatinLetter(normalized, offset)) {
          current.appendCodePoint(codePoint);
          continue;
        }
        flush(tokens, current);
        currentClass = null;
        continue;
      }

      if (codePoint == '_') {
        // LIKE wildcard이므로 토큰으로 보존한다. 식별자 중간 `_`는 접합 유지.
        if (!current.isEmpty()
            && (currentClass == CharClass.LATIN
                || currentClass == CharClass.DIGIT
                || currentClass == CharClass.HANGUL)) {
          current.append('_');
        } else {
          flush(tokens, current);
          addToken(tokens, "_");
          currentClass = null;
        }
        continue;
      }

      if (codePoint == '%' || codePoint == '％') {
        // LIKE wildcard / 도수 표기. 앞 토큰에 붙이거나 단독 토큰으로 보존한다.
        if (!current.isEmpty()) {
          current.append('%');
        } else {
          addToken(tokens, "%");
        }
        currentClass = null;
        continue;
      }

      CharClass nextClass = classify(codePoint);
      if (!current.isEmpty() && currentClass != null && currentClass != nextClass) {
        flush(tokens, current);
      }

      current.appendCodePoint(codePoint);
      currentClass = nextClass;
    }

    flush(tokens, current);
    return List.copyOf(new LinkedHashSet<>(tokens));
  }

  public static List<String> tokenizeAll(Collection<String> inputs) {
    if (inputs == null || inputs.isEmpty()) {
      return List.of();
    }

    List<String> tokens = new ArrayList<>();
    for (String input : inputs) {
      tokens.addAll(tokenize(input));
    }
    return List.copyOf(new LinkedHashSet<>(tokens));
  }

  public static String normalizeDocument(String... fields) {
    if (fields == null || fields.length == 0) {
      return "";
    }

    List<String> tokens = new ArrayList<>();
    for (String field : fields) {
      tokens.addAll(tokenize(field));
    }
    return String.join(" ", new LinkedHashSet<>(tokens));
  }

  private static void flush(List<String> tokens, StringBuilder current) {
    if (current.isEmpty()) {
      return;
    }
    addToken(tokens, current.toString());
    current.setLength(0);
  }

  private static void addToken(List<String> tokens, String token) {
    if (token != null && !token.isBlank()) {
      tokens.add(token);
    }
  }

  private static CharClass classify(int codePoint) {
    if (isHangul(codePoint)) {
      return CharClass.HANGUL;
    }
    if (isLatin(codePoint)) {
      return CharClass.LATIN;
    }
    if (Character.isDigit(codePoint)) {
      return CharClass.DIGIT;
    }
    return CharClass.OTHER;
  }

  private static boolean isHangul(int codePoint) {
    Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
    return script == Character.UnicodeScript.HANGUL
        || (codePoint >= 0xAC00 && codePoint <= 0xD7A3)
        || (codePoint >= 0x1100 && codePoint <= 0x11FF)
        || (codePoint >= 0x3130 && codePoint <= 0x318F);
  }

  /** ASCII뿐 아니라 {@code Smögen}, {@code Nàdurra} 같은 라틴 확장 문자도 한 단어로 유지한다. */
  private static boolean isLatin(int codePoint) {
    return Character.isLetter(codePoint)
        && Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN;
  }

  /** 다음 글자가 라틴 한 글자이고 그 뒤에 라틴 글자가 이어지지 않는지({@code E.H.}는 유지, {@code E.Pepper}는 분리) 확인한다. */
  private static boolean nextIsSingleLatinLetter(String text, int offset) {
    if (offset >= text.length()) {
      return false;
    }
    int next = text.codePointAt(offset);
    if (!isLatin(next)) {
      return false;
    }
    int after = offset + Character.charCount(next);
    return after >= text.length() || !isLatin(text.codePointAt(after));
  }

  private static boolean isAbbreviationJoiner(int codePoint) {
    return codePoint == '.' || codePoint == '&' || codePoint == '＆' || codePoint == '/';
  }

  /** {@code d}, {@code d.e}, {@code w&y}처럼 라틴 한 글자가 joiner로만 이어진 형태인지 확인한다. */
  private static boolean isSingleLetterAbbreviation(StringBuilder current) {
    if (current.isEmpty()) {
      return false;
    }
    boolean expectLetter = true;
    for (int i = 0; i < current.length(); ) {
      int cp = current.codePointAt(i);
      i += Character.charCount(cp);
      if (expectLetter ? !isLatin(cp) : !isAbbreviationJoiner(cp)) {
        return false;
      }
      expectLetter = !expectLetter;
    }
    return !expectLetter;
  }

  private static boolean isWhitespace(int codePoint) {
    return Character.isWhitespace(codePoint) || codePoint == 0x3000;
  }

  private static boolean isSeparator(int codePoint) {
    return switch (codePoint) {
      case '\'',
          '’',
          '‘',
          '`',
          '´',
          '&',
          '＆',
          '-',
          '‐',
          '‑',
          '‒',
          '–',
          '—',
          '−',
          '/',
          '／',
          '.',
          '#',
          '°',
          '(',
          ')',
          '（',
          '）',
          '[',
          ']',
          ',',
          '，',
          ':',
          '：',
          '+',
          '×',
          '✕',
          '✖',
          '*',
          '"',
          '“',
          '”' ->
          true;
      default -> false;
    };
  }

  private static boolean isIgnorable(int codePoint) {
    return codePoint < 0x20 || Character.getType(codePoint) == Character.CONTROL;
  }
}
