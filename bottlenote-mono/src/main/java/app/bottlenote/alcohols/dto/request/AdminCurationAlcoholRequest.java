package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * Admin 큐레이션 위스키 추가 요청
 *
 * @param alcoholIds 추가할 위스키 ID 목록
 */
public record AdminCurationAlcoholRequest(
    @NotEmpty(message = "추가할 위스키 ID는 최소 1개 이상이어야 합니다.") Set<Long> alcoholIds) {}
