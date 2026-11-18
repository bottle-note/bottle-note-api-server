package app.bottlenote.mfds.dto.response;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import java.time.LocalDateTime;

/** RCNO별 수입사 연결 근거 응답 항목. */
public record MfdsRcnoLinkItem(
    String rcno,
    Long importerId,
    String sourceImporterName,
    MfdsImporterLinkSource linkSource,
    String sourceGalleryUrl,
    LocalDateTime sourceObservedAt,
    LocalDateTime createdAt) {}
