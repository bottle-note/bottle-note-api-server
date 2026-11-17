package app.bottlenote.mfds.dto.response;

import java.util.List;

/** 부작용 없는 일괄 적용 미리보기. 증류소·지역은 생략했을 때 주류에 등록된 값으로 채운 결과다. */
public record MfdsBulkMatchingPreviewResponse(
    String alcoholNameKo,
    String alcoholNameEn,
    Long distilleryId,
    Long regionId,
    List<MfdsBulkMatchingPreviewItem> items) {}
