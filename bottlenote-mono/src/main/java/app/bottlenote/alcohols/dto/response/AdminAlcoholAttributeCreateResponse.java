package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 속성을 여러 위스키에 추가한 결과. 이미 같은 항목·값이 있으면 그 위스키는 skipped로 센다. */
public record AdminAlcoholAttributeCreateResponse(int added, int skipped, List<Long> createdIds) {}
