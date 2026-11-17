package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/** 맛 그룹에 들어간 테이스팅 태그 하나. 같은 태그는 그룹 안에서 한 번만 들어간다. */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TastingTagGroupTag {

  @Comment("테이스팅 태그 ID")
  @Column(name = "tasting_tag_id", nullable = false)
  private Long tastingTagId;

  @Comment("CORE: 대표, PERIPHERAL: 보조")
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 16)
  private TastingTagGroupRole role;

  private TastingTagGroupTag(Long tastingTagId, TastingTagGroupRole role) {
    this.tastingTagId = Objects.requireNonNull(tastingTagId, "tastingTagId");
    this.role = Objects.requireNonNull(role, "role");
  }

  public static TastingTagGroupTag of(Long tastingTagId, TastingTagGroupRole role) {
    return new TastingTagGroupTag(tastingTagId, role);
  }

  public BigDecimal weight() {
    return role.weight();
  }

  public boolean isCore() {
    return role == TastingTagGroupRole.CORE;
  }

  /** 동일성은 태그 ID로만 본다. 역할이 달라도 같은 태그면 하나다. */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TastingTagGroupTag other)) {
      return false;
    }
    return Objects.equals(tastingTagId, other.tastingTagId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tastingTagId);
  }
}
