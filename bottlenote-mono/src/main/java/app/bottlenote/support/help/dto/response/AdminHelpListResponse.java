package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.constant.HelpType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public record AdminHelpListResponse(Long totalCount, List<AdminHelpInfo> helpList) {

  public static AdminHelpListResponse of(Long totalCount, List<AdminHelpInfo> helpList) {
    return new AdminHelpListResponse(totalCount, helpList);
  }

  @Builder
  public record AdminHelpInfo(
      Long helpId,
      Long userId,
      String userNickname,
      String title,
      HelpType type,
      StatusType status,
      LocalDateTime createAt) {}
}
