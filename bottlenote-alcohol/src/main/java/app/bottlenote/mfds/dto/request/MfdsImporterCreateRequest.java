package app.bottlenote.mfds.dto.request;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import jakarta.validation.constraints.NotBlank;

/**
 * 수입사 수동 등록 요청.
 *
 * @param officialBusinessCode 식약처 공식 업소 식별 코드
 * @param licenseNo 인허가 번호
 * @param businessName 공식 수입사명
 * @param representativeName 대표자명
 * @param sourceListUrl 등록 근거가 된 공식 목록 조회 URL
 * @param description 공개 설명
 * @param adminNote 관리자 내부 메모
 * @param adminStatus 관리 상태 (생략 시 ACTIVE)
 */
public record MfdsImporterCreateRequest(
    @NotBlank(message = "공식 업소 코드는 필수입니다.") String officialBusinessCode,
    @NotBlank(message = "인허가 번호는 필수입니다.") String licenseNo,
    @NotBlank(message = "수입사명은 필수입니다.") String businessName,
    String representativeName,
    @NotBlank(message = "공식 출처 URL은 필수입니다.") String sourceListUrl,
    String description,
    String adminNote,
    MfdsImporterAdminStatus adminStatus) {}
