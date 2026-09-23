package app.bottlenote.mfds.service;

import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLink;
import app.bottlenote.mfds.domain.MfdsItem;
import app.bottlenote.mfds.domain.MfdsMatchingCandidate;
import app.bottlenote.mfds.dto.response.MfdsDeclarationDetailResponse;
import app.bottlenote.mfds.dto.response.MfdsDeclarationDetailResponse.MatchCandidate;
import app.bottlenote.mfds.dto.response.MfdsDeclarationListItem;
import app.bottlenote.mfds.dto.response.MfdsImporterItem;
import app.bottlenote.mfds.dto.response.MfdsItemDetailResponse;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholDetailResponse;
import app.bottlenote.mfds.dto.response.MfdsPublicAlcoholListItem;
import app.bottlenote.mfds.dto.response.MfdsPublicImporterItem;
import app.bottlenote.mfds.dto.response.MfdsRcnoLinkItem;
import java.util.List;

/** MFDS 엔티티 → 응답 DTO 변환. DTO-엔티티 분리 규칙에 따라 매핑은 서비스 계층이 소유한다. */
final class MfdsResponseMapper {

  private MfdsResponseMapper() {}

  static MfdsItemDetailResponse toItemDetail(MfdsItem item) {
    return new MfdsItemDetailResponse(
        item.getId(),
        item.getRcno(),
        item.getQueriedItemCode(),
        item.getQueriedItemName(),
        item.getProductDivisionName(),
        item.getImporterName(),
        item.getProductNameKo(),
        item.getProductNameEn(),
        item.getItemName(),
        item.getOverseasEstablishmentName(),
        item.getProcessedDate(),
        item.getExpiryText(),
        item.getManufactureCountryName(),
        item.getExportCountryName(),
        item.getDetailHref(),
        item.getObservedAt());
  }

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
        declaration.getProcessedDate(),
        declaration.getBaseProductNameKo(),
        declaration.getBaseProductNameEn(),
        declaration.getSkuDisplayNameKo(),
        declaration.getSkuDisplayNameEn(),
        declaration.getVolumeMl(),
        declaration.getAbvPercent(),
        declaration.getAgeYears(),
        declaration.getAlcoholCategoryKo(),
        declaration.getAlcoholCategoryEn(),
        declaration.getNormalizationStatus(),
        declaration.getImporterId(),
        declaration.getImporterBaseName(),
        declaration.getImporterLinkSource(),
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholMatchDecision(),
        declaration.getSelectedDistilleryId() != null,
        declaration.getSelectedRegionId() != null,
        declaration.getMatchedAt(),
        declaration.getCreatedAt());
  }

  static MfdsDeclarationDetailResponse toDeclarationDetail(
      MfdsDeclaration declaration,
      MfdsImporterItem importer,
      List<MfdsMatchingCandidate> matchingCandidates) {
    return new MfdsDeclarationDetailResponse(
        declaration.getId(),
        declaration.getRcno(),
        declaration.getProcessedDate(),
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
        candidates(matchingCandidates, "ALCOHOL"),
        declaration.getSelectedDistilleryId(),
        candidates(matchingCandidates, "DISTILLERY"),
        declaration.getSelectedRegionId(),
        candidates(matchingCandidates, "REGION"),
        declaration.getMatchedAt(),
        declaration.getCreatedAt(),
        declaration.getUpdatedAt());
  }

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

  static MfdsPublicAlcoholListItem toPublicAlcoholListItem(MfdsDeclaration declaration) {
    return new MfdsPublicAlcoholListItem(
        declaration.getId(),
        declaration.getRcno(),
        declaration.getProcessedDate(),
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholNameKo(),
        declaration.getAlcoholNameEn(),
        declaration.getBaseProductNameKo(),
        declaration.getBaseProductNameEn(),
        declaration.getSkuDisplayNameKo(),
        declaration.getSkuDisplayNameEn(),
        declaration.getAlcoholCategoryKo(),
        declaration.getAlcoholCategoryEn(),
        declaration.getAgeYears(),
        declaration.getSelectedDistilleryId() != null,
        declaration.getSelectedRegionId() != null,
        declaration.getExportCountryAlpha2(),
        declaration.getExportCountryNameKo(),
        declaration.getVolumeMl(),
        declaration.getAbvPercent(),
        declaration.getImporterId(),
        declaration.getImporterBaseName());
  }

  static MfdsPublicAlcoholDetailResponse toPublicAlcoholDetail(
      MfdsDeclaration declaration, MfdsPublicImporterItem importer) {
    return new MfdsPublicAlcoholDetailResponse(
        declaration.getId(),
        declaration.getRcno(),
        declaration.getProcessedDate(),
        declaration.getSelectedAlcoholId(),
        declaration.getAlcoholNameKo(),
        declaration.getAlcoholNameEn(),
        declaration.getBaseProductNameKo(),
        declaration.getBaseProductNameEn(),
        declaration.getSkuDisplayNameKo(),
        declaration.getSkuDisplayNameEn(),
        declaration.getAlcoholCategoryKo(),
        declaration.getAlcoholCategoryEn(),
        declaration.getExportCountryAlpha2(),
        declaration.getExportCountryNameKo(),
        declaration.getVolumeMl(),
        declaration.getAbvPercent(),
        declaration.getImporterId(),
        declaration.getImporterBaseName(),
        declaration.getUnitVolumeMl(),
        declaration.getPackageCount(),
        declaration.getAgeYears(),
        declaration.getVintageYear(),
        declaration.getEditionName(),
        declaration.getCaskNumber(),
        declaration.getBatchNumber(),
        declaration.getExpiryStart(),
        declaration.getExpiryEnd(),
        declaration.getManufacturerName(),
        declaration.getManufactureCountryNameKo(),
        importer);
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

  private static List<MatchCandidate> candidates(
      List<MfdsMatchingCandidate> candidates, String type) {
    return candidates.stream()
        .filter(c -> type.equals(c.getTargetType()))
        .map(c -> new MatchCandidate(c.getTargetId(), c.getRawScore()))
        .toList();
  }
}
