package app.bottlenote.global.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public record AdminResultResponse(String code, String message, Long targetId, String responseAt) {
  public static AdminResultResponse of(ResultCode code, Long targetId) {
    return new AdminResultResponse(
        code.name(),
        code.getMessage(),
        targetId,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }

  @Getter
  @RequiredArgsConstructor
  public enum ResultCode {
    ALCOHOL_CREATED("위스키가 등록되었습니다."),
    ALCOHOL_UPDATED("위스키가 수정되었습니다."),
    ALCOHOL_DELETED("위스키가 삭제되었습니다."),
    TASTING_TAG_CREATED("테이스팅 태그가 등록되었습니다."),
    TASTING_TAG_UPDATED("테이스팅 태그가 수정되었습니다."),
    TASTING_TAG_DELETED("테이스팅 태그가 삭제되었습니다."),
    TASTING_TAG_ALCOHOL_ADDED("위스키가 연결되었습니다."),
    TASTING_TAG_ALCOHOL_REMOVED("위스키 연결이 해제되었습니다."),
    ;

    private final String message;
  }
}
