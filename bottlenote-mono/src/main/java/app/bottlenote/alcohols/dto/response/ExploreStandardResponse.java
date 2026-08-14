package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 둘러보기 standard 응답. 페이지 정보는 meta.pagination에 둔다. */
public record ExploreStandardResponse(List<AlcoholDetailItem> items) {}
