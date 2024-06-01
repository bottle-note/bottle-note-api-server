package app.bottlenote.review.dto.request;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_TASTING_TAG_LENGTH;
import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_TASTING_TAG_LIST_SIZE;
import static java.util.stream.Collectors.toSet;

import app.bottlenote.common.file.upload.dto.response.ImageUploadInfo;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.exception.ReviewException;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ReviewCreateRequest(
	@NotEmpty(message = "alcohol id는 Null일 수 없습니다.")
	Long alcoholId,

	ReviewStatus status,

	@NotEmpty(message = "리뷰 내용을 입력해주세요")
	@Size(max = 500)
	String content,

	SizeType sizeType,

	@DecimalMin(value = "0.0", message = "가격은 0 이상이어야 합니다.")
	BigDecimal price,

	LocationInfo locationInfo,

	@Size(max = 5, message = "이미지 URL은 최대 5개까지 가능합니다.")
	List<ImageUploadInfo> imageUrlList,

	List<String> tastingTagList

) {

	private static final int TASTING_TAG_MAX_LENGTH = 12;
	private static final int TASTING_TAG_MAX_SIZE = 10;


	public ReviewCreateRequest {
		status = status == null ? ReviewStatus.PUBLIC : status;
		tastingTagList = isValidTasingTagList(tastingTagList);
	}

	private List<String> isValidTasingTagList(List<String> tastingTagList) {

		if (tastingTagList.isEmpty()) {
			return List.of();
		} else {
			//리스트를 순회하면서 각 길이가 12 초과이면 예외 발생
			tastingTagList.forEach(tastingTag -> {
				if (tastingTag.length() > TASTING_TAG_MAX_LENGTH) {
					throw new ReviewException(INVALID_TASTING_TAG_LENGTH);
				}
			});
			//앞 뒤 공백 제거 후 중복 제거
			Set<String> uniqueTastingTagList = tastingTagList.stream()
				.map(String::trim)
				.collect(toSet());

			log.info("uniqueTastingTagList is :{}", uniqueTastingTagList);
			if (uniqueTastingTagList.size() > TASTING_TAG_MAX_SIZE) {
				throw new ReviewException(INVALID_TASTING_TAG_LIST_SIZE);
			}
			return uniqueTastingTagList.stream().toList();
		}
	}
}
