package app.bottlenote.mfds.dto.response;

import java.time.LocalDate;

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
