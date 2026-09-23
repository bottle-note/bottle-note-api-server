package app.bottlenote.mfds.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Types;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "MfdsMatchingRun")
@Table(name = "mfds_matching_runs")
public class MfdsMatchingRun {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "matcher_version", length = 64)
  private String matcherVersion;

  @JdbcTypeCode(Types.CHAR)
  @Column(name = "reference_hash", length = 64)
  private String referenceHash;

  @Column(name = "normalization_version", length = 64)
  private String normalizationVersion;

  @Column(name = "scope", length = 32)
  private String scope;

  @Column(name = "status", length = 16)
  private String status;

  @Column(name = "weights_json", columnDefinition = "json")
  private String weightsJson;

  @Column(name = "stats_json", columnDefinition = "json")
  private String statsJson;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
