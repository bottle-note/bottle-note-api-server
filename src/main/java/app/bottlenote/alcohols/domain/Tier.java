package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.domain.constant.AlcoholType;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.Comment;

@Entity
public class Tier extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("타입")
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private AlcoholType type;

	@Comment("등급명")
	@Column(name = "tier", nullable = false)
	private String tier;
}
