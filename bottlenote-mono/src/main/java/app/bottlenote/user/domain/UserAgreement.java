package app.bottlenote.user.domain;

import app.bottlenote.user.constant.AgreementAction;
import app.bottlenote.user.constant.AgreementType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity(name = "user_agreements")
@Table(name = "user_agreements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAgreement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "agreement_type", nullable = false, length = 50)
  private AgreementType type;

  @Column(name = "document_version", nullable = false, length = 50)
  private String version;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 20)
  private AgreementAction action;

  @Column(name = "recorded_at", nullable = false)
  private LocalDateTime recordedAt;

  public static UserAgreement agree(
      Long userId, AgreementType type, String version, LocalDateTime recordedAt) {
    return new UserAgreement(null, userId, type, version, AgreementAction.AGREE, recordedAt);
  }
}
