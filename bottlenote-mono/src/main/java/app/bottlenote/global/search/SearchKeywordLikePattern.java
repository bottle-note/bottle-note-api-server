package app.bottlenote.global.search;

public final class SearchKeywordLikePattern {

  public static final char ESCAPE = '!';

  private SearchKeywordLikePattern() {}

  public static String contains(String searchToken) {
    String escaped =
        searchToken
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    return "%" + escaped + "%";
  }
}
