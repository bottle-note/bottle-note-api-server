package app.bottlenote.mfds.domain;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Type;

@Getter
@Entity(name = "mfds_declaration")
@Table(name = "mfds_declarations")
@Comment("RCNO별 원본 참조와 비파괴 정제 결과")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfdsDeclaration {

  @Comment("신고 정제 데이터 ID")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("수입신고번호")
  @Column(name = "rcno", nullable = false, length = 32, unique = true)
  private String rcno;

  @Comment("선택된 원장 항목 ID")
  @Column(name = "source_item_id", nullable = false)
  private Long sourceItemId;

  @Comment("제품 기본 한글명")
  @Column(name = "base_product_name_ko", columnDefinition = "TEXT")
  private String baseProductNameKo;

  @Comment("제품 기본 영문명")
  @Column(name = "base_product_name_en", columnDefinition = "TEXT")
  private String baseProductNameEn;

  @Comment("SKU 한글 표시명")
  @Column(name = "sku_display_name_ko", columnDefinition = "TEXT")
  private String skuDisplayNameKo;

  @Comment("SKU 영문 표시명")
  @Column(name = "sku_display_name_en", columnDefinition = "TEXT")
  private String skuDisplayNameEn;

  @Comment("한글명 검색 키")
  @Column(name = "name_search_key_ko", columnDefinition = "TEXT")
  private String nameSearchKeyKo;

  @Comment("영문명 검색 키")
  @Column(name = "name_search_key_en", columnDefinition = "TEXT")
  private String nameSearchKeyEn;

  @Comment("SKU 후보 검색 키 SHA-256")
  @Column(name = "sku_candidate_key_sha256", columnDefinition = "BINARY(32)")
  private byte[] skuCandidateKeySha256;

  @Comment("용량 원문")
  @Column(name = "volume_raw", length = 255)
  private String volumeRaw;

  @Comment("총 용량(ml)")
  @Column(name = "volume_ml")
  private Integer volumeMl;

  @Comment("단위 용량(ml)")
  @Column(name = "unit_volume_ml")
  private Integer unitVolumeMl;

  @Comment("포장 수량")
  @Column(name = "package_count")
  private Integer packageCount;

  @Comment("도수 원문")
  @Column(name = "abv_raw", length = 255)
  private String abvRaw;

  @Comment("알코올 도수(%)")
  @Column(name = "abv_percent", precision = 6, scale = 3)
  private BigDecimal abvPercent;

  @Comment("원재료 함량 원문")
  @Column(name = "ingredient_percent_raw", columnDefinition = "TEXT")
  private String ingredientPercentRaw;

  @Comment("원재료 함량(%)")
  @Column(name = "ingredient_percent", precision = 6, scale = 3)
  private BigDecimal ingredientPercent;

  @Comment("프루프 원문")
  @Column(name = "proof_raw", length = 255)
  private String proofRaw;

  @Comment("프루프 값")
  @Column(name = "proof_value", precision = 7, scale = 3)
  private BigDecimal proofValue;

  @Comment("도수 유형")
  @Column(name = "strength_type", length = 64)
  private String strengthType;

  @Comment("숙성 연수 원문")
  @Column(name = "age_raw", length = 255)
  private String ageRaw;

  @Comment("숙성 연수")
  @Column(name = "age_years")
  private Short ageYears;

  @Comment("빈티지 원문")
  @Column(name = "vintage_raw", length = 255)
  private String vintageRaw;

  @Comment("빈티지 연도")
  @Column(name = "vintage_year")
  private Short vintageYear;

  @Comment("버전 표식")
  @Column(name = "version_marker", length = 64)
  private String versionMarker;

  @Comment("에디션명")
  @Column(name = "edition_name", columnDefinition = "TEXT")
  private String editionName;

  @Comment("제품 변형 표식 원문")
  @Column(name = "variant_marker_raw", length = 255)
  private String variantMarkerRaw;

  @Comment("제품 변형 표식 유형")
  @Column(name = "variant_marker_type", length = 64)
  private String variantMarkerType;

  @Comment("제품 변형 표식 값")
  @Column(name = "variant_marker_value", length = 255)
  private String variantMarkerValue;

  @Comment("원재료 코드")
  @Column(name = "material_code", length = 255)
  private String materialCode;

  @Comment("캐스크 번호")
  @Column(name = "cask_number", length = 255)
  private String caskNumber;

  @Comment("배치 번호")
  @Column(name = "batch_number", length = 255)
  private String batchNumber;

  @Comment("로트 번호")
  @Column(name = "lot_number", columnDefinition = "TEXT")
  private String lotNumber;

  @Comment("제조 번호")
  @Column(name = "manufacture_number", columnDefinition = "TEXT")
  private String manufactureNumber;

  @Comment("소비기한 원문")
  @Column(name = "expiry_raw", columnDefinition = "TEXT")
  private String expiryRaw;

  @Comment("소비기한 시작일")
  @Column(name = "expiry_start")
  private LocalDate expiryStart;

  @Comment("소비기한 종료일")
  @Column(name = "expiry_end")
  private LocalDate expiryEnd;

  @Comment("수입사 기본명")
  @Column(name = "importer_base_name", columnDefinition = "TEXT")
  private String importerBaseName;

  @Comment("수입사 검색 키")
  @Column(name = "importer_search_key", columnDefinition = "TEXT")
  private String importerSearchKey;

  @Comment("법인 유형")
  @Column(name = "legal_entity_type", length = 64)
  private String legalEntityType;

  @Comment("해외 제조업소 검색 키")
  @Column(name = "overseas_establishment_search_key", columnDefinition = "TEXT")
  private String overseasEstablishmentSearchKey;

  @Comment("연결된 수입사 ID")
  @Column(name = "importer_id")
  private Long importerId;

  @Comment("연결된 수입사")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "importer_id", insertable = false, updatable = false)
  private MfdsImporter importer;

  @Comment("수입사 연결 근거 유형")
  @Enumerated(EnumType.STRING)
  @Column(name = "importer_link_source", length = 16)
  private MfdsImporterLinkSource importerLinkSource;

  @Comment("수입사 연결 시각")
  @Column(name = "importer_linked_at")
  private LocalDateTime importerLinkedAt;

  @Comment("제조사명")
  @Column(name = "manufacturer_name", columnDefinition = "TEXT")
  private String manufacturerName;

  @Comment("주류 한글명 후보")
  @Column(name = "alcohol_name_ko", columnDefinition = "TEXT")
  private String alcoholNameKo;

  @Comment("주류 영문명 후보")
  @Column(name = "alcohol_name_en", columnDefinition = "TEXT")
  private String alcoholNameEn;

  @Comment("주류 한글 카테고리")
  @Column(name = "alcohol_category_ko", length = 64)
  private String alcoholCategoryKo;

  @Comment("주류 영문 카테고리")
  @Column(name = "alcohol_category_en", length = 64)
  private String alcoholCategoryEn;

  @Comment("주류 한글 지역명")
  @Column(name = "alcohol_region_ko", length = 128)
  private String alcoholRegionKo;

  @Comment("주류 영문 지역명")
  @Column(name = "alcohol_region_en", length = 128)
  private String alcoholRegionEn;

  @Comment("주류 도수 표시값")
  @Column(name = "alcohol_abv", length = 32)
  private String alcoholAbv;

  @Comment("캐스크 후보값")
  @Column(name = "cask_candidate", length = 255)
  private String caskCandidate;

  @Comment("증류소 한글명 후보")
  @Column(name = "distillery_name_ko_candidate", columnDefinition = "TEXT")
  private String distilleryNameKoCandidate;

  @Comment("증류소 영문명 후보")
  @Column(name = "distillery_name_en_candidate", columnDefinition = "TEXT")
  private String distilleryNameEnCandidate;

  @Comment("주류 1순위 후보 ID")
  @Column(name = "alcohol_candidate_1_id")
  private Long alcoholCandidate1Id;

  @Comment("주류 1순위 후보 점수")
  @Column(name = "alcohol_candidate_1_score", precision = 10, scale = 6)
  private BigDecimal alcoholCandidate1Score;

  @Comment("주류 2순위 후보 ID")
  @Column(name = "alcohol_candidate_2_id")
  private Long alcoholCandidate2Id;

  @Comment("주류 2순위 후보 점수")
  @Column(name = "alcohol_candidate_2_score", precision = 10, scale = 6)
  private BigDecimal alcoholCandidate2Score;

  @Comment("주류 3순위 후보 ID")
  @Column(name = "alcohol_candidate_3_id")
  private Long alcoholCandidate3Id;

  @Comment("주류 3순위 후보 점수")
  @Column(name = "alcohol_candidate_3_score", precision = 10, scale = 6)
  private BigDecimal alcoholCandidate3Score;

  @Comment("선택된 주류 ID")
  @Column(name = "selected_alcohol_id")
  private Long selectedAlcoholId;

  @Comment("증류소 1순위 후보 ID")
  @Column(name = "distillery_candidate_1_id")
  private Long distilleryCandidate1Id;

  @Comment("증류소 1순위 후보 점수")
  @Column(name = "distillery_candidate_1_score", precision = 10, scale = 6)
  private BigDecimal distilleryCandidate1Score;

  @Comment("증류소 2순위 후보 ID")
  @Column(name = "distillery_candidate_2_id")
  private Long distilleryCandidate2Id;

  @Comment("증류소 2순위 후보 점수")
  @Column(name = "distillery_candidate_2_score", precision = 10, scale = 6)
  private BigDecimal distilleryCandidate2Score;

  @Comment("증류소 3순위 후보 ID")
  @Column(name = "distillery_candidate_3_id")
  private Long distilleryCandidate3Id;

  @Comment("증류소 3순위 후보 점수")
  @Column(name = "distillery_candidate_3_score", precision = 10, scale = 6)
  private BigDecimal distilleryCandidate3Score;

  @Comment("선택된 증류소 ID")
  @Column(name = "selected_distillery_id")
  private Long selectedDistilleryId;

  @Comment("지역 1순위 후보 ID")
  @Column(name = "region_candidate_1_id")
  private Long regionCandidate1Id;

  @Comment("지역 1순위 후보 점수")
  @Column(name = "region_candidate_1_score", precision = 10, scale = 6)
  private BigDecimal regionCandidate1Score;

  @Comment("지역 2순위 후보 ID")
  @Column(name = "region_candidate_2_id")
  private Long regionCandidate2Id;

  @Comment("지역 2순위 후보 점수")
  @Column(name = "region_candidate_2_score", precision = 10, scale = 6)
  private BigDecimal regionCandidate2Score;

  @Comment("지역 3순위 후보 ID")
  @Column(name = "region_candidate_3_id")
  private Long regionCandidate3Id;

  @Comment("지역 3순위 후보 점수")
  @Column(name = "region_candidate_3_score", precision = 10, scale = 6)
  private BigDecimal regionCandidate3Score;

  @Comment("선택된 지역 ID")
  @Column(name = "selected_region_id")
  private Long selectedRegionId;

  @Comment("매칭 로직 버전")
  @Column(name = "matching_version", length = 64)
  private String matchingVersion;

  @Comment("매칭 실행 ID")
  @Column(name = "matching_run_id")
  private Long matchingRunId;

  @Comment("주류 매칭 결정")
  @Column(name = "alcohol_match_decision", length = 32)
  private String alcoholMatchDecision;

  @Comment("증류소 매칭 출처")
  @Column(name = "distillery_match_source", length = 32)
  private String distilleryMatchSource;

  @Comment("지역 매칭 출처")
  @Column(name = "region_match_source", length = 32)
  private String regionMatchSource;

  @Comment("매칭 완료 시각")
  @Column(name = "matched_at")
  private LocalDateTime matchedAt;

  @Comment("제조국 한글명")
  @Column(name = "manufacture_country_name_ko", length = 128)
  private String manufactureCountryNameKo;

  @Comment("제조국 영문명")
  @Column(name = "manufacture_country_name_en", length = 128)
  private String manufactureCountryNameEn;

  @Comment("제조국 ISO Alpha-2 코드")
  @Column(name = "manufacture_country_alpha2", length = 2, columnDefinition = "CHAR(2)")
  private String manufactureCountryAlpha2;

  @Comment("제조국 ISO Alpha-3 코드")
  @Column(name = "manufacture_country_alpha3", length = 3, columnDefinition = "CHAR(3)")
  private String manufactureCountryAlpha3;

  @Comment("수출국 한글명")
  @Column(name = "export_country_name_ko", length = 128)
  private String exportCountryNameKo;

  @Comment("수출국 영문명")
  @Column(name = "export_country_name_en", length = 128)
  private String exportCountryNameEn;

  @Comment("수출국 ISO Alpha-2 코드")
  @Column(name = "export_country_alpha2", length = 2, columnDefinition = "CHAR(2)")
  private String exportCountryAlpha2;

  @Comment("수출국 ISO Alpha-3 코드")
  @Column(name = "export_country_alpha3", length = 3, columnDefinition = "CHAR(3)")
  private String exportCountryAlpha3;

  @Comment("정규화 상태")
  @Enumerated(EnumType.STRING)
  @Column(name = "normalization_status", nullable = false, length = 32)
  private MfdsNormalizationStatus normalizationStatus;

  @Comment("정규화 로직 버전")
  @Column(name = "normalization_version", length = 64)
  private String normalizationVersion;

  @Comment("정규화 판단 근거 목록")
  @Type(JsonType.class)
  @Column(name = "normalization_reasons", nullable = false, columnDefinition = "json")
  private List<String> normalizationReasons = new ArrayList<>();

  @Comment("미해석 원문 조각 목록")
  @Type(JsonType.class)
  @Column(name = "unparsed_fragments_json", nullable = false, columnDefinition = "json")
  private List<String> unparsedFragments = new ArrayList<>();

  @Comment("정규화 완료 시각")
  @Column(name = "normalized_at")
  private LocalDateTime normalizedAt;

  @Comment("관리자 검토 상태")
  @Column(name = "review_status", nullable = false, length = 32)
  private String reviewStatus;

  @Comment("최종 검토자")
  @Column(name = "reviewed_by", length = 255)
  private String reviewedBy;

  @Comment("최종 검토 시각")
  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Comment("관리자 검토 메모")
  @Column(name = "review_note", columnDefinition = "TEXT")
  private String reviewNote;

  @Comment("생성 시각")
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Comment("수정 시각")
  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  /** 관리자 검토에 따라 정규화 상태를 전이하고 검토 이력을 남긴다. */
  public void changeNormalizationStatus(
      MfdsNormalizationStatus status, String reviewedBy, String reviewNote) {
    this.normalizationStatus = status;
    this.reviewedBy = reviewedBy;
    this.reviewNote = reviewNote;
    this.reviewedAt = LocalDateTime.now();
  }

  /** 수입사를 연결하고 연결 근거 유형과 시각을 기록한다. */
  public void linkImporter(Long importerId, MfdsImporterLinkSource linkSource) {
    this.importerId = importerId;
    this.importerLinkSource = linkSource;
    this.importerLinkedAt = LocalDateTime.now();
  }

  /** 수입사 연결을 해제한다. */
  public void unlinkImporter() {
    this.importerId = null;
    this.importerLinkSource = null;
    this.importerLinkedAt = null;
  }

  public boolean isImporterLinked() {
    return this.importerId != null;
  }

  /** 매칭 실행 결과의 상위 후보(최대 3개)를 기록한다. 목록이 3개 미만이면 남는 슬롯은 비운다. */
  public void applyMatchingCandidates(
      List<MfdsMatchCandidate> alcoholCandidates,
      List<MfdsMatchCandidate> distilleryCandidates,
      List<MfdsMatchCandidate> regionCandidates,
      String matchingVersion,
      LocalDateTime matchedAt) {
    this.alcoholCandidate1Id = candidateId(alcoholCandidates, 0);
    this.alcoholCandidate1Score = candidateScore(alcoholCandidates, 0);
    this.alcoholCandidate2Id = candidateId(alcoholCandidates, 1);
    this.alcoholCandidate2Score = candidateScore(alcoholCandidates, 1);
    this.alcoholCandidate3Id = candidateId(alcoholCandidates, 2);
    this.alcoholCandidate3Score = candidateScore(alcoholCandidates, 2);
    this.distilleryCandidate1Id = candidateId(distilleryCandidates, 0);
    this.distilleryCandidate1Score = candidateScore(distilleryCandidates, 0);
    this.distilleryCandidate2Id = candidateId(distilleryCandidates, 1);
    this.distilleryCandidate2Score = candidateScore(distilleryCandidates, 1);
    this.distilleryCandidate3Id = candidateId(distilleryCandidates, 2);
    this.distilleryCandidate3Score = candidateScore(distilleryCandidates, 2);
    this.regionCandidate1Id = candidateId(regionCandidates, 0);
    this.regionCandidate1Score = candidateScore(regionCandidates, 0);
    this.regionCandidate2Id = candidateId(regionCandidates, 1);
    this.regionCandidate2Score = candidateScore(regionCandidates, 1);
    this.regionCandidate3Id = candidateId(regionCandidates, 2);
    this.regionCandidate3Score = candidateScore(regionCandidates, 2);
    this.matchingVersion = matchingVersion;
    this.matchedAt = matchedAt;
  }

  /** 매칭을 확정한다. distillery/region은 선택 사항이며 지정하지 않으면 기존 선택이 해제된다. */
  public void confirmMatching(
      Long alcoholId,
      MfdsMatchSelectionSource alcoholSource,
      Long distilleryId,
      MfdsMatchSelectionSource distillerySource,
      Long regionId,
      MfdsMatchSelectionSource regionSource) {
    this.selectedAlcoholId = alcoholId;
    this.alcoholMatchDecision = alcoholSource != null ? alcoholSource.name() : null;
    this.selectedDistilleryId = distilleryId;
    this.distilleryMatchSource =
        distilleryId != null && distillerySource != null ? distillerySource.name() : null;
    this.selectedRegionId = regionId;
    this.regionMatchSource = regionId != null && regionSource != null ? regionSource.name() : null;
  }

  /** 확정된 매칭 선택을 해제한다. 후보와 매칭 이력(matchingVersion, matchedAt)은 유지한다. */
  public void clearMatchingSelection() {
    this.selectedAlcoholId = null;
    this.alcoholMatchDecision = null;
    this.selectedDistilleryId = null;
    this.distilleryMatchSource = null;
    this.selectedRegionId = null;
    this.regionMatchSource = null;
  }

  public List<MfdsMatchCandidate> getAlcoholCandidates() {
    return storedCandidates(
        alcoholCandidate1Id,
        alcoholCandidate1Score,
        alcoholCandidate2Id,
        alcoholCandidate2Score,
        alcoholCandidate3Id,
        alcoholCandidate3Score);
  }

  public List<MfdsMatchCandidate> getDistilleryCandidates() {
    return storedCandidates(
        distilleryCandidate1Id,
        distilleryCandidate1Score,
        distilleryCandidate2Id,
        distilleryCandidate2Score,
        distilleryCandidate3Id,
        distilleryCandidate3Score);
  }

  public List<MfdsMatchCandidate> getRegionCandidates() {
    return storedCandidates(
        regionCandidate1Id,
        regionCandidate1Score,
        regionCandidate2Id,
        regionCandidate2Score,
        regionCandidate3Id,
        regionCandidate3Score);
  }

  public boolean hasAlcoholCandidate(Long alcoholId) {
    return getAlcoholCandidates().stream().anyMatch(candidate -> candidate.id().equals(alcoholId));
  }

  public boolean hasDistilleryCandidate(Long distilleryId) {
    return getDistilleryCandidates().stream()
        .anyMatch(candidate -> candidate.id().equals(distilleryId));
  }

  public boolean hasRegionCandidate(Long regionId) {
    return getRegionCandidates().stream().anyMatch(candidate -> candidate.id().equals(regionId));
  }

  private static Long candidateId(List<MfdsMatchCandidate> candidates, int index) {
    return candidates != null && candidates.size() > index ? candidates.get(index).id() : null;
  }

  private static BigDecimal candidateScore(List<MfdsMatchCandidate> candidates, int index) {
    return candidates != null && candidates.size() > index ? candidates.get(index).score() : null;
  }

  private static List<MfdsMatchCandidate> storedCandidates(Object... idScorePairs) {
    List<MfdsMatchCandidate> candidates = new ArrayList<>();
    for (int i = 0; i < idScorePairs.length; i += 2) {
      Long id = (Long) idScorePairs[i];
      BigDecimal score = (BigDecimal) idScorePairs[i + 1];
      if (id != null) {
        candidates.add(new MfdsMatchCandidate(id, score != null ? score : BigDecimal.ZERO));
      }
    }
    return candidates;
  }
}
