package app.bottlenote.alcohols.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.Comment;

@Entity(name = "country")
public class Country {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("국가 영문명")
	@Column(name = "eng_name", nullable = false)
	private String engName;

	@Comment("국가 한글명")
	@Column(name = "kor_name", nullable = false)
	private String korName;

	@Comment("ISO 2자리 코드")
	@Column(name = "iso_alpha2", nullable = true)
	private String isoAlpha2;

	@Comment("ISO 3자리 코드")
	@Column(name = "iso_alpha3", nullable = true)
	private String isoAlpha3;

	@Comment("ISO 숫자 코드")
	@Column(name = "iso_number", nullable = true)
	private String isoNumber;
}
