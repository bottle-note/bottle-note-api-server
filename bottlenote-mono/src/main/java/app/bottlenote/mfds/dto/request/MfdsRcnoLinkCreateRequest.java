package app.bottlenote.mfds.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * RCNO별 수입사 연결 근거 등록 요청.
 *
 * @param rcno 수입신고번호
 * @param importerId 연결할 수입사 ID
 */
public record MfdsRcnoLinkCreateRequest(
    @NotBlank(message = "수입신고번호는 필수입니다.") String rcno,
    @NotNull(message = "수입사 ID는 필수입니다.") Long importerId) {}
