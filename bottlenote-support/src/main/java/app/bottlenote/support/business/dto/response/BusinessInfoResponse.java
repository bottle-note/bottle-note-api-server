package app.bottlenote.support.business.dto.response;

import app.bottlenote.support.constant.StatusType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record BusinessInfoResponse(
    Long id, String title, String content, LocalDateTime createAt, StatusType status) {
  public static BusinessInfoResponse of(
      Long id, String title, String content, LocalDateTime createAt, StatusType status) {
    return new BusinessInfoResponse(id, title, content, createAt, status);
  }
}
