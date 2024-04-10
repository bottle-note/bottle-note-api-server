package app.bottlenote.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 생성자 , 수정자 정보가 존재하는 entity의 경우 사용
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity extends BaseTimeEntity {

	@Comment("최초 생성자")
	@CreatedBy
	@Column(updatable = false, name = "createdBy")
	private String createdBy;

	@Comment("최종 수정자")
	@LastModifiedBy
	@Column(name = "lastModifiedBy")
	private String lastModifiedBy;
}
