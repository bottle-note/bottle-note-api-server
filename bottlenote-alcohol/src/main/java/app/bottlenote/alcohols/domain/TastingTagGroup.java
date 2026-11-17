package app.bottlenote.alcohols.domain;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_INVALID_CODE;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_INVALID_NAME;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import app.bottlenote.alcohols.exception.AlcoholException;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 맛 그룹. 테이스팅 태그를 결 축(피트·스모키, 셰리·건과일 …)으로 묶은 사전이며 추천의 "어떤 맛을 원하세요?" 선택지가 된다.
 *
 * <p>태그 연결은 그룹 애그리게이트가 소유한다. 그룹을 저장하면 연결이 통째로 반영된다.
 */
@Getter
@Entity(name = "tasting_tag_group")
@Table(name = "tasting_tag_groups")
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TastingTagGroup extends BaseEntity {

  private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
  private static final int NAME_MAX_LENGTH = 100;
  private static final int FAMILY_MAX_LENGTH = 100;
  private static final int DESCRIPTION_MAX_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Comment("그룹 코드. 생성 후 불변")
  @Column(name = "code", nullable = false, length = 64, updatable = false)
  private String code;

  @Comment("그룹 이름. 추천 선택지로 노출")
  @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
  private String name;

  @Comment("위스키 계열. 자유 입력")
  @Column(name = "family", length = FAMILY_MAX_LENGTH)
  private String family;

  @Comment("한 줄 설명")
  @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
  private String description;

  @Comment("노출 순서")
  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "tasting_tag_group_tags", joinColumns = @JoinColumn(name = "group_id"))
  @Builder.Default
  private Set<TastingTagGroupTag> tags = new HashSet<>();

  public static TastingTagGroup create(
      String code, String name, String family, String description, Integer displayOrder) {
    return TastingTagGroup.builder()
        .code(normalizeCode(code))
        .name(normalizeName(name))
        .family(normalizeOptional(family, FAMILY_MAX_LENGTH))
        .description(normalizeOptional(description, DESCRIPTION_MAX_LENGTH))
        .displayOrder(displayOrder != null ? displayOrder : 0)
        .build();
  }

  /** 코드는 바꾸지 않는다. */
  public void update(String name, String family, String description, Integer displayOrder) {
    this.name = normalizeName(name);
    this.family = normalizeOptional(family, FAMILY_MAX_LENGTH);
    this.description = normalizeOptional(description, DESCRIPTION_MAX_LENGTH);
    if (displayOrder != null) {
      this.displayOrder = displayOrder;
    }
  }

  /** 이미 있는 태그면 역할만 바꾼다. */
  public void putTag(Long tastingTagId, TastingTagGroupRole role) {
    TastingTagGroupTag tag = TastingTagGroupTag.of(tastingTagId, role);
    tags.remove(tag);
    tags.add(tag);
  }

  public boolean removeTag(Long tastingTagId) {
    return tags.removeIf(tag -> Objects.equals(tag.getTastingTagId(), tastingTagId));
  }

  /** 연결을 통째로 교체한다. 같은 태그가 여러 번 오면 마지막 역할이 남는다. */
  public void replaceTags(Collection<TastingTagGroupTag> newTags) {
    tags.clear();
    if (newTags != null) {
      newTags.forEach(tag -> putTag(tag.getTastingTagId(), tag.getRole()));
    }
  }

  public boolean hasTags() {
    return !tags.isEmpty();
  }

  public Optional<TastingTagGroupTag> findTag(Long tastingTagId) {
    return tags.stream()
        .filter(tag -> Objects.equals(tag.getTastingTagId(), tastingTagId))
        .findFirst();
  }

  /** 역할과 무관하게 들어 있는 태그 ID 전부 */
  public Set<Long> tagIds() {
    return tags.stream().map(TastingTagGroupTag::getTastingTagId).collect(Collectors.toSet());
  }

  public List<Long> coreTagIds() {
    return tags.stream()
        .filter(TastingTagGroupTag::isCore)
        .map(TastingTagGroupTag::getTastingTagId)
        .toList();
  }

  public Set<TastingTagGroupTag> getTags() {
    return Set.copyOf(tags);
  }

  private static String normalizeCode(String code) {
    if (code == null) {
      throw new AlcoholException(TASTING_TAG_GROUP_INVALID_CODE);
    }
    String normalized = code.trim();
    if (!CODE_PATTERN.matcher(normalized).matches()) {
      throw new AlcoholException(TASTING_TAG_GROUP_INVALID_CODE);
    }
    return normalized;
  }

  private static String normalizeName(String name) {
    if (name == null || name.isBlank() || name.trim().length() > NAME_MAX_LENGTH) {
      throw new AlcoholException(TASTING_TAG_GROUP_INVALID_NAME);
    }
    return name.trim();
  }

  private static String normalizeOptional(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
  }
}
