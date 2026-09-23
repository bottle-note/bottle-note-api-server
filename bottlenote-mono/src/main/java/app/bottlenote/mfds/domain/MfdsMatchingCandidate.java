package app.bottlenote.mfds.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Entity(name = "MfdsMatchingCandidate")
@Table(name = "mfds_matching_candidates")
public class MfdsMatchingCandidate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id")
  private Long runId;

  @Column(name = "declaration_id")
  private Long declarationId;

  @JdbcTypeCode(Types.CHAR)
  @Column(name = "stage", columnDefinition = "char(1)")
  private String stage;

  @Column(name = "target_type", length = 16)
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "rank_no", columnDefinition = "tinyint unsigned")
  private Integer rankNo;

  @Column(name = "raw_score", precision = 10, scale = 4)
  private BigDecimal rawScore;

  @Column(name = "evidence_strength", columnDefinition = "smallint unsigned")
  private Integer evidenceStrength;

  @Column(name = "target_name_ko", length = 255)
  private String targetNameKo;

  @Column(name = "target_name_en", length = 255)
  private String targetNameEn;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
