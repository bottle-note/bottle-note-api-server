package app.bottlenote.common.domain;

import app.bottlenote.common.constant.AuditPrincipalType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditPrincipal {

  @Column(name = "principal_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "principal_type", length = 30)
  private AuditPrincipalType type;

  @Column(name = "principal_email")
  private String email;
}
