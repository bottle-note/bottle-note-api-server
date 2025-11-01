package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * 생성 쿼리
 *
 * CREATE TABLE IF NOT EXISTS curation_keyword
 * (
 *     id             bigint auto_increment comment '큐레이션 키워드'
 *         primary key,
 *     name           varchar(255)                        not null comment '큐레이션 키워드명',
 *     description    text                                null comment '큐레이션 설명',
 *     is_active      tinyint(1) default 1                not null comment '활성화 여부',
 *     display_order  int        default 0                not null comment '노출 순서',
 *     create_at      timestamp  default CURRENT_TIMESTAMP null comment '최초 생성일',
 *     create_by      varchar(255)                        null comment '최초 생성자',
 *     last_modify_at timestamp  default CURRENT_TIMESTAMP null comment '최종 생성일',
 *     last_modify_by varchar(255)                        null comment '최종 생성자'
 * ) comment '큐레이션 키워드';
 *
 * CREATE TABLE IF NOT EXISTS curation_keyword_alcohol_ids
 * (
 *     curation_keyword_id bigint not null comment '큐레이션 키워드 ID',
 *     alcohol_ids         bigint not null comment '위스키 ID',
 *     KEY curation_keyword_id (curation_keyword_id)
 * ) comment '큐레이션-위스키 매핑';
 */
@Entity
@Table(name = "curation_keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CurationKeyword extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "display_order", nullable = false)
	private Integer displayOrder;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
		name = "curation_keyword_alcohol_ids",
		joinColumns = @JoinColumn(name = "curation_keyword_id")
	)
	@Column(name = "alcohol_ids")
	@Builder.Default
	private Set<Long> alcoholIds = new HashSet<>();

	public static CurationKeyword create(
		String name,
		String description,
		Integer displayOrder,
		Set<Long> alcoholIds
	) {
		return CurationKeyword.builder()
			.name(name)
			.description(description)
			.isActive(true)
			.displayOrder(displayOrder != null ? displayOrder : 0)
			.alcoholIds(alcoholIds != null ? new HashSet<>(alcoholIds) : new HashSet<>())
			.build();
	}
}
