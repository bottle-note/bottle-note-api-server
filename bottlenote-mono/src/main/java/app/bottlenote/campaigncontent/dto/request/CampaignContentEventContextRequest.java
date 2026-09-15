package app.bottlenote.campaigncontent.dto.request;

/**
 * 이벤트를 보낸 주체. 컨트롤러가 인증 정보와 방문자 필터의 요청 속성에서 채운다.
 *
 * @param userId 로그인 회원 ID, 비로그인이면 null
 * @param visitorId 방문자 쿠키의 SHA-256 해시, 방문자 필터가 꺼져 있으면 null
 * @param ipAddress 정규화한 클라이언트 IP
 * @param deviceType 정규화 기기 유형
 */
public record CampaignContentEventContextRequest(
    Long userId, String visitorId, String ipAddress, String deviceType) {}
