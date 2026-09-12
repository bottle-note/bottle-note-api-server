package app.bottlenote.mfds.dto.dsl;

import app.bottlenote.global.search.SearchKeywordTokenizer;
import app.bottlenote.mfds.dto.request.MfdsPublicAlcoholSearchRequest;
import java.time.LocalDate;
import java.util.List;

/** Product 수입 주류 공개 목록 조회 조건. Spring Data 타입을 담지 않는다. */
public record MfdsPublicAlcoholSearchCriteria(
    String alcoholNameKo,
    Long alcoholId,
    Long importerId,
    String exportCountry,
    String alcoholCategoryKo,
    LocalDate processedDateFrom,
    LocalDate processedDateTo,
    List<String> searchTokens,
    LocalDate cursorProcessedDate,
    Long cursorId,
    int fetchLimit) {

  public static MfdsPublicAlcoholSearchCriteria of(
      MfdsPublicAlcoholSearchRequest request, LocalDate cursorProcessedDate, Long cursorId) {
    String category = request.alcoholType() == null ? null : request.alcoholType().getKorCategory();
    return new MfdsPublicAlcoholSearchCriteria(
        request.alcoholNameKo(),
        request.alcoholId(),
        request.importerId(),
        request.exportCountry(),
        category,
        request.processedDateFrom(),
        request.processedDateTo(),
        SearchKeywordTokenizer.tokenize(request.keyword()),
        cursorProcessedDate,
        cursorId,
        request.size() + 1);
  }

  public boolean hasCursor() {
    return cursorId != null;
  }

  /** 커서 컨텍스트. 커서는 포함하지 않으며 request에서 바로 계산할 수 있다. */
  public static String cursorContext(MfdsPublicAlcoholSearchRequest request) {
    String category = request.alcoholType() == null ? null : request.alcoholType().getKorCategory();
    return buildCursorContext(
        request.alcoholNameKo(),
        request.alcoholId(),
        request.importerId(),
        request.exportCountry(),
        category,
        request.processedDateFrom(),
        request.processedDateTo(),
        SearchKeywordTokenizer.tokenize(request.keyword()));
  }

  public String cursorContext() {
    return buildCursorContext(
        alcoholNameKo,
        alcoholId,
        importerId,
        exportCountry,
        alcoholCategoryKo,
        processedDateFrom,
        processedDateTo,
        searchTokens);
  }

  private static String buildCursorContext(
      String alcoholNameKo,
      Long alcoholId,
      Long importerId,
      String exportCountry,
      String alcoholCategoryKo,
      LocalDate processedDateFrom,
      LocalDate processedDateTo,
      List<String> searchTokens) {
    return "mfds.public.alcohols:"
        + nullToEmpty(alcoholNameKo)
        + ":"
        + nullToEmpty(alcoholId)
        + ":"
        + nullToEmpty(importerId)
        + ":"
        + nullToEmpty(exportCountry)
        + ":"
        + nullToEmpty(alcoholCategoryKo)
        + ":"
        + nullToEmpty(processedDateFrom)
        + ":"
        + nullToEmpty(processedDateTo)
        + ":"
        + String.join(" ", searchTokens);
  }

  private static String nullToEmpty(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
