package app.bottlenote.banner.domain;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.constant.TextPosition;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Comment("홈 배너")
@Entity(name = "banner")
@Table(name = "banners")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("배너명")
	@Column(name = "name", nullable = false)
	private String name;

	@Comment("이미지 URL")
	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Comment("텍스트 위치")
	@Column(name = "text_position", nullable = false)
	@Enumerated(EnumType.STRING)
	private TextPosition textPosition;

	@Comment("클릭 시 이동 URL")
	@Column(name = "target_url")
	private String targetUrl;

	@Comment("외부 URL 여부")
	@Column(name = "is_external_url", nullable = false)
	private Boolean isExternalUrl = false;

	@Comment("배너 유형")
	@Column(name = "banner_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private BannerType bannerType;

	@Comment("정렬 순서")
	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder = 0;

	@Comment("노출 시작일")
	@Column(name = "start_date")
	private LocalDate startDate;

	@Comment("노출 종료일")
	@Column(name = "end_date")
	private LocalDate endDate;

	@Comment("활성화 여부")
	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	@Builder
	public Banner(
		Long id,
		String name,
		String imageUrl,
		TextPosition textPosition,
		String targetUrl,
		Boolean isExternalUrl,
		BannerType bannerType,
		Integer sortOrder,
		LocalDate startDate,
		LocalDate endDate,
		Boolean isActive
	) {
		this.id = id;
		this.name = name;
		this.imageUrl = imageUrl;
		this.textPosition = textPosition;
		this.targetUrl = targetUrl;
		this.isExternalUrl = isExternalUrl != null ? isExternalUrl : false;
		this.bannerType = bannerType;
		this.sortOrder = sortOrder != null ? sortOrder : 0;
		this.startDate = startDate;
		this.endDate = endDate;
		this.isActive = isActive != null ? isActive : true;
	}
}
