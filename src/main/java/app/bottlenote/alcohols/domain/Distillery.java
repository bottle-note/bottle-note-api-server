package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Comment("증류소")
@Entity(name = "distillery")
public class Distillery extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("증류소 영어 이름")
	@Column(name = "eng_name", nullable = false)
	private String engName;

	@Comment("증류소 한글 이름")
	@Column(name = "kor_name", nullable = false)
	private String korName;

	@Comment("증류소 로고 이미지 경로")
	@Column(name = "logo_img_path")
	private String logoImgPath;

	@OneToMany(mappedBy = "distillery")
	private List<Alcohols> alcohols = new ArrayList<>();
}
