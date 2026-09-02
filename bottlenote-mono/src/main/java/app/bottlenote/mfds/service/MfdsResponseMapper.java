package app.bottlenote.mfds.service;

import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLink;
import app.bottlenote.mfds.dto.response.MfdsDeclarationDetailResponse;
import app.bottlenote.mfds.dto.response.MfdsDeclarationDetailResponse.MatchCandidate;
import app.bottlenote.mfds.dto.response.MfdsDeclarationListItem;
import app.bottlenote.mfds.dto.response.MfdsImporterItem;
import app.bottlenote.mfds.dto.response.MfdsRcnoLinkItem;
import app.bottlenote.mfds.facade.payload.MfdsPublicDeclarationItem;
import app.bottlenote.mfds.facade.payload.MfdsPublicImporterItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** MFDS 엔티티 → 응답 DTO 변환. DTO-엔티티 분리 규칙에 따라 매핑은 서비스 계층이 소유한다. */
final class MfdsResponseMapper {

  private MfdsResponseMapper() {}

  static MfdsImporterItem toImporterItem(MfdsImporter importer) {
    return new MfdsImporterItem(
        importer.getId(),
        importer.getOfficialBusinessCode(),
        importer.getLicenseNo(),
        importer.getBusinessName(),
        importer.getRepresentativeName(),
        importer.getPermitDate(),
        importer.getInstitutionName(),
        importer.getPrimaryAddress(),
        importer.getTelephoneNo(),
        importer.getIndustryName(),
        importer.getOperatingStatus(),
        importer.getDescription(),
        importer.getAdminNote(),
        importer.getAdminStatus(),
        importer.getReviewedBy(),
        importer.getReviewedAt(),
        importer.getCreatedAt(),
        importer.getUpdatedAt());
  }

  static MfdsDeclarationListItem toDeclarationListItem(MfdsDeclaration declaration) {
    return new MfdsDeclarationListItem(
        declaration.getId(),
        declaration.getRcno(),
        declaration.getBaseProductNameKo(),
        declaration.getBaseProductNameEn(),
        declaration.getSkuDisplayNameKo(),
        declaration.getSkuDisplayNameEn(),
        declaration.getVolumeMl(),
        declaration.getAbvPercent(),
        declaration.getNormalizationStatus(),
        declaration.getImporterId(),
        declaration.getImporterBaseName(),
        declaration.getImporterLinkSource(),
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholMatchDecision(),
        declaration.getMatchedAt(),
        declaration.getCreatedAt());
  }

  static MfdsDeclarationDetailResponse toDeclarationDetail(
      MfdsDeclaration declaration, MfdsImporterItem importer) {
    return new MfdsDeclarationDetailResponse(
        declaration.getId(),
        declaration.getRcno(),
        declaration.getBaseProductNameKo(),
        declaration.getBaseProductNameEn(),
        declaration.getSkuDisplayNameKo(),
        declaration.getSkuDisplayNameEn(),
        declaration.getVolumeRaw(),
        declaration.getVolumeMl(),
        declaration.getUnitVolumeMl(),
        declaration.getPackageCount(),
        declaration.getAbvRaw(),
        declaration.getAbvPercent(),
        declaration.getAgeYears(),
        declaration.getVintageYear(),
        declaration.getEditionName(),
        declaration.getCaskNumber(),
        declaration.getBatchNumber(),
        declaration.getExpiryStart(),
        declaration.getExpiryEnd(),
        declaration.getImporterBaseName(),
        declaration.getManufacturerName(),
        declaration.getAlcoholNameKo(),
        declaration.getAlcoholNameEn(),
        declaration.getAlcoholCategoryKo(),
        declaration.getAlcoholCategoryEn(),
        declaration.getManufactureCountryNameKo(),
        declaration.getExportCountryNameKo(),
        declaration.getNormalizationStatus(),
        declaration.getNormalizationReasons(),
        declaration.getUnparsedFragments(),
        declaration.getNormalizedAt(),
        declaration.getReviewStatus(),
        declaration.getReviewedBy(),
        declaration.getReviewedAt(),
        declaration.getReviewNote(),
        declaration.getImporterLinkSource(),
        declaration.getImporterLinkedAt(),
        importer,
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholMatchDecision(),
        candidates(
            declaration.getAlcoholCandidate1Id(), declaration.getAlcoholCandidate1Score(),
            declaration.getAlcoholCandidate2Id(), declaration.getAlcoholCandidate2Score(),
            declaration.getAlcoholCandidate3Id(), declaration.getAlcoholCandidate3Score()),
        declaration.getSelectedDistilleryId(),
        candidates(
            declaration.getDistilleryCandidate1Id(), declaration.getDistilleryCandidate1Score(),
            declaration.getDistilleryCandidate2Id(), declaration.getDistilleryCandidate2Score(),
            declaration.getDistilleryCandidate3Id(), declaration.getDistilleryCandidate3Score()),
        declaration.getSelectedRegionId(),
        candidates(
            declaration.getRegionCandidate1Id(), declaration.getRegionCandidate1Score(),
            declaration.getRegionCandidate2Id(), declaration.getRegionCandidate2Score(),
            declaration.getRegionCandidate3Id(), declaration.getRegionCandidate3Score()),
        declaration.getMatchedAt(),
        declaration.getCreatedAt(),
        declaration.getUpdatedAt());
  }

  static MfdsRcnoLinkItem toRcnoLinkItem(MfdsImporterRcnoLink link) {
    return new MfdsRcnoLinkItem(
        link.getRcno(),
        link.getImporterId(),
        link.getSourceImporterName(),
        link.getLinkSource(),
        link.getSourceGalleryUrl(),
        link.getSourceObservedAt(),
        link.getCreatedAt());
  }

  /** Product 공개용 수입사. Admin 항목과 필드명을 맞추고 내부 운영 필드는 제외한다. */
  static MfdsPublicImporterItem toPublicImporterItem(MfdsImporter importer) {
    return new MfdsPublicImporterItem(
        importer.getId(),
        importer.getOfficialBusinessCode(),
        importer.getLicenseNo(),
        importer.getBusinessName(),
        importer.getRepresentativeName(),
        importer.getPermitDate(),
        importer.getInstitutionName(),
        importer.getPrimaryAddress(),
        importer.getTelephoneNo(),
        importer.getIndustryName(),
        importer.getOperatingStatus(),
        importer.getDescription());
  }

  /** Product 공개용 신고. Admin 상세와 공개 필드명·importer 중첩을 맞춘다. */
  static MfdsPublicDeclarationItem toPublicDeclarationItem(
      MfdsDeclaration declaration, MfdsPublicImporterItem importer) {
    return new MfdsPublicDeclarationItem(
        declaration.getId(),
        declaration.getRcno(),
        declaration.getBaseProductNameKo(),
        declaration.getBaseProductNameEn(),
        declaration.getSkuDisplayNameKo(),
        declaration.getSkuDisplayNameEn(),
        declaration.getVolumeMl(),
        declaration.getUnitVolumeMl(),
        declaration.getPackageCount(),
        declaration.getAbvPercent(),
        declaration.getAgeYears(),
        declaration.getVintageYear(),
        declaration.getEditionName(),
        declaration.getCaskNumber(),
        declaration.getBatchNumber(),
        declaration.getExpiryStart(),
        declaration.getExpiryEnd(),
        declaration.getImporterBaseName(),
        declaration.getManufacturerName(),
        declaration.getAlcoholNameKo(),
        declaration.getAlcoholNameEn(),
        declaration.getAlcoholCategoryKo(),
        declaration.getAlcoholCategoryEn(),
        declaration.getManufactureCountryNameKo(),
        declaration.getExportCountryNameKo(),
        importer);
  }

  private static List<MatchCandidate> candidates(
      Long id1, BigDecimal score1, Long id2, BigDecimal score2, Long id3, BigDecimal score3) {
    List<MatchCandidate> result = new ArrayList<>();
    if (id1 != null) {
      result.add(new MatchCandidate(id1, score1));
    }
    if (id2 != null) {
      result.add(new MatchCandidate(id2, score2));
    }
    if (id3 != null) {
      result.add(new MatchCandidate(id3, score3));
    }
    return result;
  }
}
