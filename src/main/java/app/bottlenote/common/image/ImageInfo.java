package app.bottlenote.common.image;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageInfo {

	@Comment("이미지 순서")
	@Column(name = "`order`", nullable = false)
	private Long order;

	@Comment("이미지 경로")
	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Comment("업로드 된 루트 경로(버킷부터 이미지 이름까지)")
	@Column(name = "image_key", nullable = false)
	private String imageKey;

	@Comment("저장된 이미지 경로(버킷부터 최종폴더까지)")
	@Column(name = "image_path", nullable = false)
	private String imagePath;

	@Comment("생성된 UUID + 확장자 파일명")
	@Column(name = "image_name", nullable = false)
	private String imageName;

	@Builder
	public ImageInfo(Long order, String imageUrl, String imageKey, String imagePath, String imageName) {
		this.order = order;
		this.imageUrl = imageUrl;
		this.imageKey = imageKey;
		this.imagePath = imagePath;
		this.imageName = imageName;
	}
}
