package app.bottlenote.support.business.dto.response;

import app.bottlenote.support.business.constant.BusinessSupportType;
import app.bottlenote.support.business.dto.request.BusinessImageItem;
import app.bottlenote.support.constant.StatusType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record BusinessSupportDetailItem(
    Long id,
    String title,
    String content,
    String contact,
    BusinessSupportType businessSupportType,
    List<BusinessImageItem> imageUrlList,
    LocalDateTime createAt,
    StatusType status,
    Long adminId,
    String responseContent,
    LocalDateTime lastModifyAt) {}
