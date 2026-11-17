package app.bottlenote.mfds.dto.request;

import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 수입 신고 정규화 상태 변경 요청.
 *
 * @param normalizationStatus 전이할 정규화 상태
 * @param reviewedBy 검토자 표시명
 * @param reviewNote 검토 메모
 */
public record MfdsDeclarationStatusRequest(
    @NotNull(message = "정규화 상태는 필수입니다.") MfdsNormalizationStatus normalizationStatus,
    String reviewedBy,
    String reviewNote) {}
