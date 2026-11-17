package app.bottlenote.mfds.dto.dsl;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;

/**
 * 수입사 목록 조회 포트 기준(id-desc keyset). Spring/JPA 타입을 포함하지 않는다.
 *
 * <p>cursor 미지정/0이면 조건 없음. cursor &gt; 0이면 {@code id < cursor}.
 */
public record MfdsImporterSearchCriteria(
    MfdsImporterAdminStatus adminStatus, String keyword, Long cursor, Long pageSize) {

  public static final long DEFAULT_SIZE = 20L;
  public static final long MAX_SIZE = 100L;

  public MfdsImporterSearchCriteria {
    cursor = cursor == null || cursor < 0L ? 0L : cursor;
    pageSize = pageSize == null || pageSize < 1L ? DEFAULT_SIZE : Math.min(pageSize, MAX_SIZE);
    keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
  }

  public static MfdsImporterSearchCriteria of(Long cursor, Long pageSize) {
    return new MfdsImporterSearchCriteria(null, null, cursor, pageSize);
  }

  /** 다음 페이지 keyset 조건 사용 여부. 0 이하면 최초 페이지. */
  public boolean hasCursor() {
    return cursor != null && cursor > 0L;
  }

  /** hasNext 판별을 위해 pageSize + 1건을 조회한다. */
  public long fetchLimit() {
    return pageSize + 1;
  }
}
