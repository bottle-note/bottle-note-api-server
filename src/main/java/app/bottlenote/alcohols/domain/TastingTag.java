package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity(name = "tasting_tag")
public class TastingTag extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("태그 영어 이름")
    @Column(name = "eng_name", nullable = false)
    private String engName;

    @Comment("태그 한글 이름")
    @Column(name = "kor_name", nullable = false)
    private String korName;

    @Comment("아이콘")
    @Column(name = "icon")
    private String icon;

    @Comment("태그 설명")
    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "tastingTag")
    private List<AlcoholsTastingTags> alcoholsTastingTags = new ArrayList<>();

}
