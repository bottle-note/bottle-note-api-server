package app.bottlenote.mfds.domain;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "rcno", nullable = false, length = 32, unique = true)
  private String rcno;

  @Column(name = "source_item_id", nullable = false)
  private Long sourceItemId;

  @Column(name = "base_product_name_ko", columnDefinition = "TEXT")
  private String baseProductNameKo;

  @Column(name = "base_product_name_en", columnDefinition = "TEXT")
  private String baseProductNameEn;

  @Column(name = "sku_display_name_ko", columnDefinition = "TEXT")
  private String skuDisplayNameKo;

  @Column(name = "sku_display_name_en", columnDefinition = "TEXT")
  private String skuDisplayNameEn;

  @Column(name = "name_search_key_ko", columnDefinition = "TEXT")
  private String nameSearchKeyKo;

  @Column(name = "name_search_key_en", columnDefinition = "TEXT")
  private String nameSearchKeyEn;

  @Column(name = "sku_candidate_key_sha256", columnDefinition = "BINARY(32)")
  private byte[] skuCandidateKeySha256;

  @Column(name = "volume_raw", length = 255)
  private String volumeRaw;

  @Column(name = "volume_ml")
  private Integer volumeMl;

  @Column(name = "unit_volume_ml")
  private Integer unitVolumeMl;

  @Column(name = "package_count")
  private Integer packageCount;

  @Column(name = "abv_raw", length = 255)
  private String abvRaw;

  @Column(name = "abv_percent", precision = 6, scale = 3)
  private BigDecimal abvPercent;

  @Column(name = "ingredient_percent_raw", columnDefinition = "TEXT")
  private String ingredientPercentRaw;

  @Column(name = "ingredient_percent", precision = 6, scale = 3)
  private BigDecimal ingredientPercent;

  @Column(name = "proof_raw", length = 255)
  private String proofRaw;

  @Column(name = "proof_value", precision = 7, scale = 3)
  private BigDecimal proofValue;

  @Column(name = "strength_type", length = 64)
  private String strengthType;

  @Column(name = "age_raw", length = 255)
  private String ageRaw;

  @Column(name = "age_years")
  private Short ageYears;

  @Column(name = "vintage_raw", length = 255)
  private String vintageRaw;

  @Column(name = "vintage_year")
  private Short vintageYear;

  @Column(name = "version_marker", length = 64)
  private String versionMarker;

  @Column(name = "edition_name", columnDefinition = "TEXT")
  private String editionName;

  @Column(name = "variant_marker_raw", length = 255)
  private String variantMarkerRaw;

  @Column(name = "variant_marker_type", length = 64)
  private String variantMarkerType;

  @Column(name = "variant_marker_value", length = 255)
  private String variantMarkerValue;

  @Column(name = "material_code", length = 255)
  private String materialCode;

  @Column(name = "cask_number", length = 255)
  private String caskNumber;

  @Column(name = "batch_number", length = 255)
  private String batchNumber;

  @Column(name = "lot_number", columnDefinition = "TEXT")
  private String lotNumber;

  @Column(name = "manufacture_number", columnDefinition = "TEXT")
  private String manufactureNumber;

  @Column(name = "expiry_raw", columnDefinition = "TEXT")
  private String expiryRaw;

  @Column(name = "expiry_start")
  private LocalDate expiryStart;

  @Column(name = "expiry_end")
  private LocalDate expiryEnd;

  @Column(name = "importer_base_name", columnDefinition = "TEXT")
  private String importerBaseName;

  @Column(name = "importer_search_key", columnDefinition = "TEXT")
  private String importerSearchKey;

  @Column(name = "legal_entity_type", length = 64)
  private String legalEntityType;

  @Column(name = "overseas_establishment_search_key", columnDefinition = "TEXT")
  private String overseasEstablishmentSearchKey;

  @Column(name = "importer_id")
  private Long importerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "importer_link_source", length = 16)
  private MfdsImporterLinkSource importerLinkSource;

  @Column(name = "importer_linked_at")
  private LocalDateTime importerLinkedAt;

  @Column(name = "manufacturer_name", columnDefinition = "TEXT")
  private String manufacturerName;

  @Column(name = "alcohol_name_ko", columnDefinition = "TEXT")
  private String alcoholNameKo;

  @Column(name = "alcohol_name_en", columnDefinition = "TEXT")
  private String alcoholNameEn;

  @Column(name = "alcohol_category_ko", length = 64)
  private String alcoholCategoryKo;

  @Column(name = "alcohol_category_en", length = 64)
  private String alcoholCategoryEn;

  @Column(name = "alcohol_region_ko", length = 128)
  private String alcoholRegionKo;

  @Column(name = "alcohol_region_en", length = 128)
  private String alcoholRegionEn;

  @Column(name = "alcohol_abv", length = 32)
  private String alcoholAbv;

  @Column(name = "cask_candidate", length = 255)
  private String caskCandidate;

  @Column(name = "distillery_name_ko_candidate", columnDefinition = "TEXT")
  private String distilleryNameKoCandidate;

  @Column(name = "distillery_name_en_candidate", columnDefinition = "TEXT")
  private String distilleryNameEnCandidate;

  @Column(name = "alcohol_candidate_1_id")
  private Long alcoholCandidate1Id;

  @Column(name = "alcohol_candidate_1_score", precision = 10, scale = 6)
  private BigDecimal alcoholCandidate1Score;

  @Column(name = "alcohol_candidate_2_id")
  private Long alcoholCandidate2Id;

  @Column(name = "alcohol_candidate_2_score", precision = 10, scale = 6)
  private BigDecimal alcoholCandidate2Score;

  @Column(name = "alcohol_candidate_3_id")
  private Long alcoholCandidate3Id;

  @Column(name = "alcohol_candidate_3_score", precision = 10, scale = 6)
  private BigDecimal alcoholCandidate3Score;

  @Column(name = "selected_alcohol_id")
  private Long selectedAlcoholId;

  @Column(name = "distillery_candidate_1_id")
  private Long distilleryCandidate1Id;

  @Column(name = "distillery_candidate_1_score", precision = 10, scale = 6)
  private BigDecimal distilleryCandidate1Score;

  @Column(name = "distillery_candidate_2_id")
  private Long distilleryCandidate2Id;

  @Column(name = "distillery_candidate_2_score", precision = 10, scale = 6)
  private BigDecimal distilleryCandidate2Score;

  @Column(name = "distillery_candidate_3_id")
  private Long distilleryCandidate3Id;

  @Column(name = "distillery_candidate_3_score", precision = 10, scale = 6)
  private BigDecimal distilleryCandidate3Score;

  @Column(name = "selected_distillery_id")
  private Long selectedDistilleryId;

  @Column(name = "region_candidate_1_id")
  private Long regionCandidate1Id;

  @Column(name = "region_candidate_1_score", precision = 10, scale = 6)
  private BigDecimal regionCandidate1Score;

  @Column(name = "region_candidate_2_id")
  private Long regionCandidate2Id;

  @Column(name = "region_candidate_2_score", precision = 10, scale = 6)
  private BigDecimal regionCandidate2Score;

  @Column(name = "region_candidate_3_id")
  private Long regionCandidate3Id;

  @Column(name = "region_candidate_3_score", precision = 10, scale = 6)
  private BigDecimal regionCandidate3Score;

  @Column(name = "selected_region_id")
  private Long selectedRegionId;

  @Column(name = "matching_version", length = 64)
  private String matchingVersion;

  @Column(name = "matching_run_id")
  private Long matchingRunId;

  @Column(name = "alcohol_match_decision", length = 32)
  private String alcoholMatchDecision;

  @Column(name = "distillery_match_source", length = 32)
  private String distilleryMatchSource;

  @Column(name = "region_match_source", length = 32)
  private String regionMatchSource;

  @Column(name = "matched_at")
  private LocalDateTime matchedAt;

  @Column(name = "manufacture_country_name_ko", length = 128)
  private String manufactureCountryNameKo;

  @Column(name = "manufacture_country_name_en", length = 128)
  private String manufactureCountryNameEn;

  @Column(name = "manufacture_country_alpha2", length = 2)
  private String manufactureCountryAlpha2;

  @Column(name = "manufacture_country_alpha3", length = 3)
  private String manufactureCountryAlpha3;

  @Column(name = "export_country_name_ko", length = 128)
  private String exportCountryNameKo;

  @Column(name = "export_country_name_en", length = 128)
  private String exportCountryNameEn;

  @Column(name = "export_country_alpha2", length = 2)
  private String exportCountryAlpha2;

  @Column(name = "export_country_alpha3", length = 3)
  private String exportCountryAlpha3;

  @Enumerated(EnumType.STRING)
  @Column(name = "normalization_status", nullable = false, length = 32)
  private MfdsNormalizationStatus normalizationStatus;

  @Column(name = "normalization_version", length = 64)
  private String normalizationVersion;

  @Type(JsonType.class)
  @Column(name = "normalization_reasons", nullable = false, columnDefinition = "json")
  private List<String> normalizationReasons = new ArrayList<>();

  @Type(JsonType.class)
  @Column(name = "unparsed_fragments_json", nullable = false, columnDefinition = "json")
  private List<String> unparsedFragments = new ArrayList<>();

  @Column(name = "normalized_at")
  private LocalDateTime normalizedAt;

  @Column(name = "review_status", nullable = false, length = 32)
  private String reviewStatus;

  @Column(name = "reviewed_by", length = 255)
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "review_note", columnDefinition = "TEXT")
  private String reviewNote;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}
