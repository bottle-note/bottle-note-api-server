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

  /** 커서 컨텍스트. 커서는 포함하지 않으며 request에서 바로 계산할 수 있다. */
  public static String cursorContext(MfdsPublicImporterSearchRequest request) {
    return "mfds.public.importers:"
        + String.join(" ", SearchKeywordTokenizer.tokenize(request.keyword()));
  }

  public String cursorContext() {
    return "mfds.public.importers:" + String.join(" ", searchTokens);
  }
}
