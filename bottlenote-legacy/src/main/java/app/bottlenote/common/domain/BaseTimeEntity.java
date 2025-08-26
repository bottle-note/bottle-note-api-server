package app.bottlenote.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 생성일, 수정일이 필요한 entity의 경우 사용 */
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
}
