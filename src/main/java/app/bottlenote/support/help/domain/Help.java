package app.bottlenote.support.help.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.domain.constant.HelpType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Comment("문의사항")
@Entity(name = "help")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Help extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("문의자")
	@Column(name = "userId", nullable = false)
	private Long userId;

	@Comment("문의 타입")
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private HelpType type;

	@Comment("문의 내용")
	@Column(name = "help_content", nullable = false)
	private String content;

	@Comment("문의 이미지")
	@OneToMany(mappedBy = "help", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HelpImage> helpImages = new ArrayList<>();

	@Comment("문의글의 처리 상태")
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private StatusType status = StatusType.WAITING;

	@Comment("담당자( 추후 Admin으로 변경)")
	@Column(name = "admin_id", nullable = true)
	private Long adminId;

	@Comment("응답 내용")
	@Column(name = "response_content", nullable = false)
	private String responseContent;

	@Builder
	private Help(Long id, Long userId, HelpType type, String content, Long adminId, String responseContent) {
		this.id = id;
		this.userId = userId;
		this.type = type;
		this.content = content;
		this.adminId = adminId;
		this.responseContent = responseContent;
		this.helpImages = new ArrayList<>();
	}

	public static Help create(Long userId, HelpType helpType, String content) {
		return Help.builder()
			.userId(userId)
			.type(helpType)
			.content(content)
			.build();
	}

	public void saveImages(List<HelpImage> images) {
		HelpImageList helpImageList = new HelpImageList(this.helpImages);
		helpImageList.addImages(images);
		this.helpImages = helpImageList.getHelpImages();
	}

	public void updateImages(List<HelpImage> helpImages) {
		this.helpImages.clear();
		this.helpImages.addAll(helpImages);
	}

	public void updateHelp(String content, HelpType helpType){
		Objects.requireNonNull(content, "content는 필수입니다");
		Objects.requireNonNull(helpType, "helpType은 필수입니다");
		this.content = content;
		this.type = helpType;
	}

	public void deleteHelp(){
		this.status = StatusType.DELETED;
	}

	public boolean isMyHelpPost(Long userId) {
		return this.userId.equals(userId);
	}
}
