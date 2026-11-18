package app.bottlenote.common.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 생성일, 수정일과 감사 주체가 필요한 entity의 공통 클래스 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseTimeEntity extends AbstractAggregateRoot<BaseEntity> {

  @Comment("최초 생성일")
  @CreatedDate
  @Column(updatable = false, name = "create_at")
  private LocalDateTime createAt;

  @Comment("최종 수정일")
  @LastModifiedDate
  @Column(name = "last_modify_at")
  private LocalDateTime lastModifyAt;

  @CreatedBy
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(
        name = "id",
        column = @Column(name = "create_principal_id", updatable = false)),
    @AttributeOverride(
        name = "type",
        column = @Column(name = "create_principal_type", length = 30, updatable = false)),
    @AttributeOverride(
        name = "email",
        column = @Column(name = "create_principal_email", updatable = false))
  })
  private AuditPrincipal createPrincipal;

  @LastModifiedBy
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "id", column = @Column(name = "last_modify_principal_id")),
    @AttributeOverride(
        name = "type",
        column = @Column(name = "last_modify_principal_type", length = 30)),
    @AttributeOverride(name = "email", column = @Column(name = "last_modify_principal_email"))
  })
  private AuditPrincipal lastModifyPrincipal;
}
