package app.bottlenote.global.search;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class SearchKeywordTokenizer {

  private SearchKeywordTokenizer() {}

  public static List<String> tokenize(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return List.of();
    }
    return List.copyOf(
        new LinkedHashSet<>(
            Arrays.stream(keyword.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList()));
  }
}
