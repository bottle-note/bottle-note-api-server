package app.bottlenote.curation.domain;

import app.bottlenote.common.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Type;

@Comment("spec 기반 큐레이션 payload")
@Entity(name = "curation_extension")
@Table(name = "curation_extension")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CurationExtension extends BaseEntity {

  @Id
  @Comment("큐레이션 ID")
  @Column(name = "curation_id")
  private Long curationId;

  @Comment("큐레이션 스펙 ID")
  @Column(name = "spec_id", nullable = false)
  private Long specId;

  @Comment("request spec 검증을 통과한 payload")
  @Column(name = "payload", nullable = false, columnDefinition = "json")
  @Type(JsonType.class)
  private Object payload;

  public void update(Long specId, Object payload) {
    this.specId = specId;
    this.payload = payload;
  }
}
