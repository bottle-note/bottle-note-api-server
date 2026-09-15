package app.bottlenote.campaigncontent.domain;

import java.util.List;

/** 참여 지표에서 뺄 기기 유형과 IP 대역. 값은 방문자 통계 제외 규칙에서 받아온다. */
public record CampaignContentExclusion(List<String> deviceTypes, List<String> ipPrefixes) {

  public CampaignContentExclusion {
    deviceTypes = deviceTypes == null ? List.of() : List.copyOf(deviceTypes);
    ipPrefixes = ipPrefixes == null ? List.of() : List.copyOf(ipPrefixes);
  }
}
