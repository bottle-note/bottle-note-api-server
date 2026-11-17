package app.bottlenote.curation.domain;

import app.bottlenote.common.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Type;

@Comment("spec 기반 큐레이션 스펙")
@Entity(name = "curation_spec")
@Table(name = "curation_spec")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CurationSpec extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("스펙 코드")
  @Column(name = "code", nullable = false, length = 80)
  private String code;

  @Comment("스펙 표시명")
  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Comment("스펙 설명")
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Builder.Default
  @Comment("OpenAPI request schema")
  @Column(name = "request_spec", nullable = false, columnDefinition = "json")
  @Type(JsonType.class)
  private Map<String, Object> requestSpec = new LinkedHashMap<>();

  @Builder.Default
  @Comment("OpenAPI response schema")
  @Column(name = "response_spec", nullable = false, columnDefinition = "json")
  @Type(JsonType.class)
  private Map<String, Object> responseSpec = new LinkedHashMap<>();

  @Comment("GraphQL hydration 식별자")
  @Column(name = "hydrator_key", nullable = false, length = 80)
  private String hydratorKey;

  @Builder.Default
  @Comment("스펙 버전")
  @Column(name = "version", nullable = false)
  private Integer version = 1;

  @Builder.Default
  @Comment("활성화 여부")
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  public void update(
      String name,
      String description,
      Map<String, Object> requestSpec,
      Map<String, Object> responseSpec,
      String hydratorKey,
      Integer version,
      Boolean isActive) {
    this.name = name;
    this.description = description;
    this.requestSpec = copyOf(requestSpec);
    this.responseSpec = copyOf(responseSpec);
    this.hydratorKey = hydratorKey;
    this.version = version != null ? version : this.version;
    this.isActive = isActive != null ? isActive : this.isActive;
  }

  private static Map<String, Object> copyOf(Map<String, Object> value) {
    return value != null ? new LinkedHashMap<>(value) : new LinkedHashMap<>();
  }
}
