package app.bottlenote.mfds.dto.response;

import java.time.LocalDate;

/**
 * Product 공개용 수입사 정보.
 *
 * <p>필드명은 Admin {@link MfdsImporterItem}과 맞추고, 관리 메모·검토·상태 등 내부 필드는 제외한다.
 */
public record MfdsPublicImporterItem(
    Long id,
    String officialBusinessCode,
    String licenseNo,
    String businessName,
    String representativeName,
    LocalDate permitDate,
    String institutionName,
    String primaryAddress,
    String telephoneNo,
    String industryName,
    String operatingStatus,
    String description) {}
