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
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

// 재생성은 feed_payload만 바꾼다. 전체 컬럼 UPDATE면 로드 시점의 stale payload가 어드민 저장분을 덮는다.
@DynamicUpdate
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

  @Comment("x-feed 기준 피드 조회용 파생 payload")
  @Column(name = "feed_payload", columnDefinition = "json")
  @Type(JsonType.class)
  private Object feedPayload;

  public void update(Long specId, Object payload, Object feedPayload) {
    this.specId = specId;
    this.payload = payload;
    this.feedPayload = feedPayload;
  }

  // 스펙 변경에 따른 재생성용. 원본 payload는 SSOT이므로 건드리지 않는다.
  public void updateFeedPayload(Object feedPayload) {
    this.feedPayload = feedPayload;
  }

  // 피드 조회 소스. NULL은 backfill 이전 레거시 행이므로 원본으로 되돌아간다.
  // 빈 결과는 []/{}로 저장되므로 NULL만 fallback 조건이다.
  public Object feedSource() {
    return feedPayload != null ? feedPayload : payload;
  }
}
