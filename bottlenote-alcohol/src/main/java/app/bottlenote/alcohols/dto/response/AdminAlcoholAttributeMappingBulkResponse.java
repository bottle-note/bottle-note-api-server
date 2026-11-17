package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 주류와 속성의 매핑 결과. 기존 연결은 skipped로 세며 createdIds는 신규 매핑 ID다. */
public record AdminAlcoholAttributeMappingBulkResponse(
    int added, int skipped, List<Long> createdIds) {}
