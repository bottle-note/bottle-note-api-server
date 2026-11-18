package app.bottlenote.mfds.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "MfdsMatchingEvidence")
@Table(name = "mfds_matching_evidence")
public class MfdsMatchingEvidence {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "candidate_id")
  private Long candidateId;

  @Column(name = "feature_code", length = 64)
  private String featureCode;

  @Column(name = "evidence_source", length = 64)
  private String evidenceSource;

  @Column(name = "input_value", columnDefinition = "text")
  private String inputValue;

  @Column(name = "reference_value", columnDefinition = "text")
  private String referenceValue;

  @Column(name = "rule_code", length = 64)
  private String ruleCode;

  @Column(name = "weight", precision = 10, scale = 4)
  private BigDecimal weight;

  @Column(name = "upstream_target_id")
  private Long upstreamTargetId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
