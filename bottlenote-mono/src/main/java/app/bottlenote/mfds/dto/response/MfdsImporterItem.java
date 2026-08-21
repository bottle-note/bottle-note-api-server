package app.bottlenote.mfds.dto.response;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 수입사 목록·상세 공용 응답 항목. */
public record MfdsImporterItem(
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
    String description,
    String adminNote,
    MfdsImporterAdminStatus adminStatus,
    String reviewedBy,
    LocalDateTime reviewedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
