package app.bottlenote.support.business.domain;

import app.bottlenote.common.image.ImageInfo;
import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.support.business.dto.request.BusinessImageItem;
import app.bottlenote.support.business.exception.BusinessSupportException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_IMAGE_URL_MAX_SIZE;

@Embeddable
@RequiredArgsConstructor
@Getter
public class BusinessImageList {

	@Comment("비즈니스 문의 이미지")
	@OneToMany(
		mappedBy = "businessSupportId",
		fetch = FetchType.LAZY,
		cascade = CascadeType.ALL,
		orphanRemoval = true)
	private List<BusinessImage> businessImages = new ArrayList<>();

	private void validateOverMaxSize(List<BusinessImage> businessImageList) {
		if (businessImageList.size() > 5) {
			throw new BusinessSupportException(INVALID_IMAGE_URL_MAX_SIZE);
		}
	}

	public void addImages(List<BusinessImageItem> images, Long businessSupportId) {
		this.businessImages.addAll(makeBusinessImageList(images, businessSupportId));
	}

	public void clear() {
		this.businessImages.clear();
	}

	public List<BusinessImage> makeBusinessImageList(List<BusinessImageItem> images, Long businessSupportId) {
		List<BusinessImage> businessImageList = images.stream()
			.map(image -> BusinessImage.builder()
				.businessImageInfo(
					ImageInfo.builder()
						.order(image.order())
						.imageUrl(image.viewUrl())
						.imagePath(ImageUtil.getImagePath(image.viewUrl()))
						.imageKey(ImageUtil.getImageKey(image.viewUrl()))
						.imageName(ImageUtil.getImageName(image.viewUrl()))
						.build()
				)
				.businessSupportId(businessSupportId)
				.build())
			.toList();

		validateOverMaxSize(businessImageList);

		return businessImageList;
	}
}