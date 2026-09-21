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
    boolean apostrophePending = false;

    String normalized = input.trim().toLowerCase(Locale.ROOT);
    for (int offset = 0; offset < normalized.length(); ) {
      int codePoint = normalized.codePointAt(offset);
      offset += Character.charCount(codePoint);

      if (isIgnorable(codePoint)) {
        apostrophePending = false;
        continue;
      }

      if (isApostrophe(codePoint)) {
        apostrophePending = currentClass == CharClass.LATIN && !current.isEmpty();
        continue;
      }

      if (isWhitespace(codePoint) || isSeparator(codePoint)) {
        flush(tokens, current);
        currentClass = null;
        apostrophePending = false;
        if (codePoint == '&' || codePoint == '＆') {
          addToken(tokens, "and");
        }
        continue;
      }

      if (codePoint == '_') {
        if (!current.isEmpty()
            && (currentClass == CharClass.LATIN
                || currentClass == CharClass.DIGIT
                || currentClass == CharClass.HANGUL)) {
          current.appendCodePoint(codePoint);
        } else {
          flush(tokens, current);
          currentClass = null;
        }
        apostrophePending = false;
        continue;
      }

      CharClass nextClass = classify(codePoint);
      if (apostrophePending) {
        if (nextClass != CharClass.LATIN || currentClass != CharClass.LATIN) {
          flush(tokens, current);
          currentClass = null;
        }
        apostrophePending = false;
      }

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

  private static boolean isLatin(int codePoint) {
    return (codePoint >= 'a' && codePoint <= 'z') || (codePoint >= 'A' && codePoint <= 'Z');
  }

  private static boolean isWhitespace(int codePoint) {
    return Character.isWhitespace(codePoint) || codePoint == 0x3000;
  }

  private static boolean isApostrophe(int codePoint) {
    return codePoint == '\''
        || codePoint == '’'
        || codePoint == '‘'
        || codePoint == '`'
        || codePoint == '´';
  }

  private static boolean isSeparator(int codePoint) {
    return switch (codePoint) {
      case '&',
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
          '%',
          '％',
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
