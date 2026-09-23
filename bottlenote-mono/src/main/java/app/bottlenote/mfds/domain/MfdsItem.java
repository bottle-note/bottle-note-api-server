package app.bottlenote.mfds.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** 수집기 소유 원장에서 비교에 필요한 컬럼만 읽는다. */
@Getter
@Immutable
@Entity(name = "mfds_item")
@Table(name = "mfds_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfdsItem {

  @Id private Long id;

  @Column(nullable = false, length = 32)
  private String rcno;

  @Column(name = "queried_item_code", nullable = false, length = 32)
  private String queriedItemCode;

  @Column(name = "queried_item_name", nullable = false, length = 100)
  private String queriedItemName;

  @Column(name = "product_division_name", length = 255)
  private String productDivisionName;

  @Column(name = "importer_name", length = 512)
  private String importerName;

  @Column(name = "product_name_ko", length = 1024)
  private String productNameKo;

  @Column(name = "product_name_en", length = 1024)
  private String productNameEn;

  @Column(name = "item_name", length = 255)
  private String itemName;

  @Column(name = "overseas_establishment_name", length = 1024)
  private String overseasEstablishmentName;

  @Column(name = "processed_date")
  private LocalDate processedDate;

  @Column(name = "expiry_text", length = 512)
  private String expiryText;

  @Column(name = "manufacture_country_name", length = 255)
  private String manufactureCountryName;

  @Column(name = "export_country_name", length = 255)
  private String exportCountryName;

  @Column(name = "detail_href", columnDefinition = "TEXT")
  private String detailHref;

  @Column(name = "observed_at", nullable = false)
  private LocalDateTime observedAt;
}
