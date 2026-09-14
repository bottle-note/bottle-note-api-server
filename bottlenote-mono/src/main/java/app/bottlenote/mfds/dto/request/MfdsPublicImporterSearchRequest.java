package app.bottlenote.mfds.dto.request;

import app.bottlenote.global.pagination.KeysetPageRequest;

/** Product 수입사 목록 검색 요청. */
public record MfdsPublicImporterSearchRequest(String keyword, String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public MfdsPublicImporterSearchRequest {
    keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
