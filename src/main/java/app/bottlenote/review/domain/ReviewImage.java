package app.bottlenote.review.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Entity
@Getter
public class ReviewImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("이미지 경로")
	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Comment("파일명")
	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Comment("파일 크기")
	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	@Comment("이미지 순서")
	@Column(name = "order", nullable = false)
	private Long order;

	@Comment("상태")
	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private ImageStatus status;

	@Comment("태그")
	@Column(name = "tags")
	private String tags;

	@Comment("주석")
	@Column(name = "description")
	private String description;
}
