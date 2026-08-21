package app.bottlenote.mfds.dto.request;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 수입사 관리 항목 수정 요청.
 *
 * @param businessName 공식 수입사명
 * @param description 공개 설명
 * @param adminNote 관리자 내부 메모
 * @param adminStatus 관리 상태
 */
public record MfdsImporterUpdateRequest(
    @NotBlank(message = "수입사명은 필수입니다.") String businessName,
    String description,
    String adminNote,
    @NotNull(message = "관리 상태는 필수입니다.") MfdsImporterAdminStatus adminStatus) {}
