package app.bottlenote.statistics.domain;

/**
 * 기간 안의 퍼널 단계별 인원.
 *
 * @param visitors 기간 안 고유 방문자 수
 * @param signedUp 기간 안 가입한 회원 수
 * @param agreed 가입 회원 중 필수 약관 최신 이력이 동의인 수
 * @param activated 가입 회원 중 리뷰·별점·찜 중 하나라도 남긴 수
 */
public record MemberFunnelCounts(long visitors, long signedUp, long agreed, long activated) {

  public static MemberFunnelCounts empty() {
    return new MemberFunnelCounts(0L, 0L, 0L, 0L);
  }
}
