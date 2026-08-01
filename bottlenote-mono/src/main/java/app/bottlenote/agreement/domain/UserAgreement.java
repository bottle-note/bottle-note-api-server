package app.bottlenote.agreement.domain;

import app.bottlenote.agreement.constant.AgreementAction;
import app.bottlenote.agreement.constant.AgreementInputContext;
import app.bottlenote.agreement.constant.AgreementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;

@Getter
@Immutable
@Entity(name = "userAgreement")
@Table(name = "user_agreements")
@Comment("사용자 약관·개인정보 동의 이력")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAgreement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(updatable = false)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "agreement_type", nullable = false, length = 50, updatable = false)
  private AgreementType agreementType;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 20, updatable = false)
  private AgreementAction action;

  @Column(
      name = "document_content",
      nullable = false,
      columnDefinition = "mediumtext",
      updatable = false)
  private String documentContent;

  @Column(name = "recorded_at", nullable = false, updatable = false)
  private LocalDateTime recordedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "input_context", nullable = false, length = 20, updatable = false)
  private AgreementInputContext inputContext;

  @Column(name = "client_ip", length = 45, updatable = false)
  private String clientIp;

  @Column(name = "user_agent", length = 512, updatable = false)
  private String userAgent;

  private UserAgreement(
      Long userId,
      AgreementType agreementType,
      AgreementAction action,
      String documentContent,
      LocalDateTime recordedAt,
      AgreementInputContext inputContext,
      String clientIp,
      String userAgent) {
    this.userId = Objects.requireNonNull(userId, "userId은 null일 수 없습니다.");
    this.agreementType = Objects.requireNonNull(agreementType, "agreementType은 null일 수 없습니다.");
    this.action = Objects.requireNonNull(action, "action은 null일 수 없습니다.");
    this.documentContent =
        Objects.requireNonNull(documentContent, "documentContent는 null일 수 없습니다.");
    this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt은 null일 수 없습니다.");
    this.inputContext = Objects.requireNonNull(inputContext, "inputContext는 null일 수 없습니다.");
    this.clientIp = clientIp;
    this.userAgent = userAgent;
  }

  public static UserAgreement create(
      Long userId,
      AgreementType agreementType,
      AgreementAction action,
      String documentContent,
      LocalDateTime recordedAt,
      AgreementInputContext inputContext,
      String clientIp,
      String userAgent) {
    return new UserAgreement(
        userId,
        agreementType,
        action,
        documentContent,
        recordedAt,
        inputContext,
        clientIp,
        userAgent);
  }
}
