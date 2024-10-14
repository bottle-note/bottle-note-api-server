package app.bottlenote.support.help.domain;

import app.bottlenote.support.help.exception.HelpException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_IMAGE_URL_MAX_SIZE;

@Getter
@RequiredArgsConstructor
public class HelpImageList {

	private List<HelpImage> helpImages;

	public HelpImageList(List<HelpImage> helpImageList) {
		validateOverMaxSize(helpImageList);
		this.helpImages = helpImageList;
	}

	private void validateOverMaxSize(List<HelpImage> helpImageList) {
		if (helpImageList.size() > 5) {
			throw new HelpException(INVALID_IMAGE_URL_MAX_SIZE);
		}
	}

	public void addImages(List<HelpImage> images) {
		this.helpImages.addAll(images);
	}
}
