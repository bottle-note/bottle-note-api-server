package app.bottlenote.mfds.dto.request;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.global.pagination.KeysetPageRequest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/** Product 수입 주류 목록 검색 요청. */
public record MfdsPublicAlcoholSearchRequest(
    String alcoholNameKo,
    Long alcoholId,
    Long importerId,
    @Pattern(regexp = "^[A-Z]{2}$", message = "INVALID_EXPORT_COUNTRY_PATTERN")
        String exportCountry,
    AlcoholType alcoholType,
    LocalDate processedDateFrom,
    LocalDate processedDateTo,
    String keyword,
    String cursor,
    Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public MfdsPublicAlcoholSearchRequest {
    alcoholNameKo = blankToNull(alcoholNameKo);
    exportCountry = blankToNull(exportCountry == null ? null : exportCountry.trim().toUpperCase());
    keyword = blankToNull(keyword);
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }

  @AssertTrue(message = "processedDateFrom는 processedDateTo보다 이후일 수 없습니다.")
  public boolean isProcessedDateRangeValid() {
    return processedDateFrom == null
        || processedDateTo == null
        || !processedDateFrom.isAfter(processedDateTo);
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
