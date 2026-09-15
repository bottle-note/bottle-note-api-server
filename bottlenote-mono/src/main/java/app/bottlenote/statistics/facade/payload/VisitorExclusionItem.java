package app.bottlenote.statistics.facade.payload;

import java.util.List;

/** 방문자 통계에서 빼는 기기 유형과 IP 대역. 루트 관리자 제외는 조회 SQL이 직접 처리한다. */
public record VisitorExclusionItem(List<String> deviceTypes, List<String> ipPrefixes) {}
