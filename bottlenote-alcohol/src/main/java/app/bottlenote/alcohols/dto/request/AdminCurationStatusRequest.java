package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Admin 큐레이션 활성화 상태 변경 요청
 *
 * @param isActive 활성화 상태
 */
public record AdminCurationStatusRequest(@NotNull(message = "활성화 상태는 필수입니다.") Boolean isActive) {}
