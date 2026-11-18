package app.bottlenote.global.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AdminResultResponse(String code, String message, Long targetId, String responseAt) {
  public static AdminResultResponse of(AdminResultCode code, Long targetId) {
    return new AdminResultResponse(
        code.name(),
        code.getMessage(),
        targetId,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }
}
