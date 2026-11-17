package app.bottlenote.mfds.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 수입 신고에 수입사를 수동 연결하는 요청.
 *
 * @param importerId 연결할 수입사 ID
 */
public record MfdsDeclarationImporterLinkRequest(
    @NotNull(message = "수입사 ID는 필수입니다.") Long importerId) {}
