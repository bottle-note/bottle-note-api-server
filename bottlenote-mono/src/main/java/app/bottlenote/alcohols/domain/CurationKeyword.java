package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "curation_keyword")
@Table(name = "curation_keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CurationKeyword extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "cover_image_url")
  private String coverImageUrl;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "curation_keyword_alcohol_ids",
      joinColumns = @JoinColumn(name = "curation_keyword_id"))
  @Column(name = "alcohol_ids")
  @Builder.Default
  private Set<Long> alcoholIds = new HashSet<>();

  public static CurationKeyword create(
      String name, String description, String coverImageUrl, Integer displayOrder, Set<Long> alcoholIds) {
    return CurationKeyword.builder()
        .name(name)
        .description(description)
        .coverImageUrl(coverImageUrl)
        .isActive(true)
        .displayOrder(displayOrder != null ? displayOrder : 0)
        .alcoholIds(alcoholIds != null ? new HashSet<>(alcoholIds) : new HashSet<>())
        .build();
  }
}
