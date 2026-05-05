package app.bottlenote.alcohols.dto.response;

import app.bottlenote.global.service.cursor.CursorResponse;

/**
 * 둘러보기 standard 서비스 응답 묶음. seed 는 RANDOM 정렬에서 실제 사용된 값(요청값 또는 서버 생성값)이며, 컨트롤러가 응답 meta 에 실어 클라이언트에
 * 에코한다. 비-RANDOM 정렬에서는 의미 없음(서비스가 0 으로 채워 전달).
 */
public record ExploreStandardResponse(long seed, CursorResponse<AlcoholDetailItem> page) {}
