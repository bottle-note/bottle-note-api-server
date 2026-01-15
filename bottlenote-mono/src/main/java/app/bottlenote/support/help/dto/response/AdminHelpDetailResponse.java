package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpImageItem;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record AdminHelpDetailResponse(
    Long helpId,
    Long userId,
    String userNickname,
    String title,
    String content,
    HelpType type,
    List<HelpImageItem> imageUrlList,
    StatusType status,
    Long adminId,
    String responseContent,
    LocalDateTime createAt,
    LocalDateTime lastModifyAt) {}
