package app.bottlenote.mfds.dto.response;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 수입 신고 상세 응답. 연결 수입사와 주류·증류소·지역 매칭 후보를 포함한다. */
public record MfdsDeclarationDetailResponse(
    Long id,
    String rcno,
    String baseProductNameKo,
    String baseProductNameEn,
    String skuDisplayNameKo,
    String skuDisplayNameEn,
    String volumeRaw,
    Integer volumeMl,
    Integer unitVolumeMl,
    Integer packageCount,
    String abvRaw,
    BigDecimal abvPercent,
    Short ageYears,
    Short vintageYear,
    String editionName,
    String caskNumber,
    String batchNumber,
    LocalDate expiryStart,
    LocalDate expiryEnd,
    String importerBaseName,
    String manufacturerName,
    String alcoholNameKo,
    String alcoholNameEn,
    String alcoholCategoryKo,
    String alcoholCategoryEn,
    String manufactureCountryNameKo,
    String exportCountryNameKo,
    MfdsNormalizationStatus normalizationStatus,
    List<String> normalizationReasons,
    List<String> unparsedFragments,
    LocalDateTime normalizedAt,
    String reviewStatus,
    String reviewedBy,
    LocalDateTime reviewedAt,
    String reviewNote,
    MfdsImporterLinkSource importerLinkSource,
    LocalDateTime importerLinkedAt,
    MfdsImporterItem importer,
    Long selectedAlcoholId,
    String alcoholMatchDecision,
    List<MatchCandidate> alcoholCandidates,
    Long selectedDistilleryId,
    List<MatchCandidate> distilleryCandidates,
    Long selectedRegionId,
    List<MatchCandidate> regionCandidates,
    LocalDateTime matchedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  /** 매칭 후보 항목. 순위는 리스트 순서를 따른다. */
  public record MatchCandidate(Long candidateId, BigDecimal score) {}
}
