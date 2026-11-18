package app.bottlenote.support.help.dto.request;

import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.constant.HelpType;
import lombok.Builder;

/**
 * @param page 페이지 번호 (0부터)
 * @param size 페이지 크기
 */
public record AdminHelpPageableRequest(
    StatusType status, HelpType type, Integer page, Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public AdminHelpPageableRequest {
    page = page != null && page >= 0 ? page : 0;
    if (size == null || size < 1) {
      size = DEFAULT_SIZE;
    } else {
      size = Math.min(size, MAX_SIZE);
    }
  }
}
