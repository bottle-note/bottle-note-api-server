package app.bottlenote.support.help.domain;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_IMAGE_URL_MAX_SIZE;

import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.support.help.dto.request.HelpImageInfo;
import app.bottlenote.support.help.exception.HelpException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Comment;

@Embeddable
@RequiredArgsConstructor
public class HelpImageList {

	@Comment("문의 이미지")
	@OneToMany(
		mappedBy = "helpId",
		fetch = FetchType.LAZY,
		cascade = CascadeType.ALL,
		orphanRemoval = true)
	private List<HelpImage> helpImages = new ArrayList<>();

	private void validateOverMaxSize(List<HelpImage> helpImageList) {
		if (helpImageList.size() > 5) {
			throw new HelpException(INVALID_IMAGE_URL_MAX_SIZE);
		}
	}

	public void addImages(List<HelpImageInfo> images, Long helpId) {
		this.helpImages.addAll(makeHelpImageList(images, helpId));
	}

	public void clear() {
		this.helpImages.clear();
	}

	public List<HelpImage> makeHelpImageList(List<HelpImageInfo> images, Long helpId) {
		List<HelpImage> helpImageList = images.stream()
			.map(image -> HelpImage.builder()
				.order(image.order())
				.imageUrl(image.viewUrl())
				.imagePath(ImageUtil.getImagePath(image.viewUrl()))
				.imageKey(ImageUtil.getImageKey(image.viewUrl()))
				.imageName(ImageUtil.getImageName(image.viewUrl()))
				.helpId(helpId)
				.build())
			.toList();

		validateOverMaxSize(helpImageList);

		return helpImageList;
	}
}
