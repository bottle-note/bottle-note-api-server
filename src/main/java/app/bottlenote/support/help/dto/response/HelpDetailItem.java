package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpImageItem;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record HelpDetailItem(
    Long helpId,
    String title,
    String content,
    HelpType helpType,
    List<HelpImageItem> imageUrlList,
    LocalDateTime createAt,
    StatusType statusType,
    Long adminId,
    String responseContent,
    LocalDateTime lastModifyAt) {}
