package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.domain.constant.AlcoholType;
import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.review.domain.Review;
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
import jakarta.persistence.OneToMany;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity(name = "alcohol")
public class Alcohol extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("알코올 한글 이름")
	@Column(name = "kor_name", nullable = false)
	private String korName;

	@Comment("알코올 영어 이름")
	@Column(name = "eng_name", nullable = false)
	private String engName;

	@Comment("도수")
	@Column(name = "abv", nullable = true)
	private String abv;

	@Comment("타입")
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private AlcoholType type;

	@Comment("하위 카테고리 한글명 ( ex. 위스키, 럼 )")
	@Column(name = "kor_category", nullable = false)
	private String korCategory;

	@Comment("하위 카테고리 영문명 ( ex. 위스키, 럼 )")
	@Column(name = "eng_category", nullable = false)
	private String engCategory;

	@Comment("국가")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region_id", nullable = true)
	private Region region;

	@Comment("증류소")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "distillery_id")
	private Distillery distillery;

	@Comment("캐스트 타입")
	@Column(name = "cask", nullable = true)
	private String cask;

	@Comment("썸네일 이미지")
	@Column(name = "image_url", nullable = true)
	private String imageUrl;

	// mappedBy: 연관관계의 주인이 아님을 의미한다.
	// Review가 alcohol의 id를 가지고 있다.
	@OneToMany(mappedBy = "alcohol", fetch = FetchType.LAZY)
	private List<Review> reviews = new ArrayList<>();

	// mappedBy: 연관관계의 주인이 아님을 의미한다.
	// AlcoholsTastingTags가 alcohol의 id를 가지고 있으므로 mappedBy를 사용한다.
	@OneToMany(mappedBy = "alcohol", fetch = FetchType.LAZY)
	private Set<AlcoholsTastingTags> alcoholsTastingTags = new HashSet<>();

	// mappedBy: 연관관계의 주인이 아님을 의미한다.
	// Rating이 alcohol의 id를 가지고 있다.
	@OneToMany(mappedBy = "alcohol", fetch = FetchType.LAZY)
	private List<Rating> rating = new ArrayList<>();

	@Override
	public String toString() {
		return "Alcohol{" +
				"id=" + id +
				", korName='" + korName + '\'' +
				", engName='" + engName + '\'' +
				", abv='" + abv + '\'' +
				", type=" + type +
				", korCategory='" + korCategory + '\'' +
				", engCategory='" + engCategory + '\'' +
				", region=" + region +
				", distillery=" + distillery +
				", cask='" + cask + '\'' +
				", imageUrl='" + imageUrl + '\'' +
				'}';
	}
}
