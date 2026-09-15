package app.bottlenote.campaigncontent.domain;

import app.bottlenote.campaigncontent.constant.CampaignContentEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/** 추가 전용 참여 기록이라 감사 컬럼 없이 발생 시각만 둔다. */
@Comment("캠페인 콘텐츠 참여 이벤트")
@Entity(name = "campaign_content_event_log")
@Table(name = "campaign_content_events")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CampaignContentEventLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("캠페인 콘텐츠 ID")
  @Column(name = "campaign_content_id", nullable = false, updatable = false)
  private Long campaignContentId;

  @Comment("이벤트 유형")
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 16, updatable = false)
  private CampaignContentEventType eventType;

  @Comment("방문자 쿠키의 SHA-256 해시")
  @Column(name = "visitor_id", length = 64, updatable = false)
  private String visitorId;

  @Comment("회원 ID")
  @Column(name = "user_id", updatable = false)
  private Long userId;

  @Comment("정규화한 클라이언트 IP")
  @Column(name = "ip_address", length = 45, updatable = false)
  private String ipAddress;

  @Comment("정규화 기기 유형")
  @Column(name = "device_type", length = 20, updatable = false)
  private String deviceType;

  @Comment("이벤트 발생 시각")
  @Column(name = "occurred_at", nullable = false, updatable = false)
  private LocalDateTime occurredAt;
}
