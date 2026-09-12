package app.bottlenote.mfds.dto.dsl;

import app.bottlenote.global.search.SearchKeywordTokenizer;
import app.bottlenote.mfds.dto.request.MfdsPublicImporterSearchRequest;
import java.util.List;

/** Product 수입사 공개 목록 조회 조건. */
public record MfdsPublicImporterSearchCriteria(
    List<String> searchTokens, Long cursorId, int fetchLimit) {

  public static MfdsPublicImporterSearchCriteria of(
      MfdsPublicImporterSearchRequest request, Long cursorId) {
    return new MfdsPublicImporterSearchCriteria(
        SearchKeywordTokenizer.tokenize(request.keyword()), cursorId, request.size() + 1);
  }

  public boolean hasCursor() {
    return cursorId != null;
  }

  public String cursorContext() {
    return "mfds.public.importers:" + String.join(" ", searchTokens);
  }
}
