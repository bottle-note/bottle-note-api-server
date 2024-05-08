package app.bottlenote.image.service;

import static app.bottlenote.image.exception.ImageExceptionCode.FAILED_SAVE_IMAGE;
import static app.bottlenote.image.exception.ImageExceptionCode.INVALID_CONTENT_TYPE;

import app.bottlenote.image.constant.ImageType;
import app.bottlenote.image.dto.response.ImageUrlResponse;
import app.bottlenote.image.exception.ImageException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImageService {

	private static final String DELETE_SUCCESS = "프로필 사진이 삭제되었습니다.";

	private final S3UploadService s3UploadService;

	public ImageUrlResponse saveProfileImages(MultipartFile image) {

		if (image.isEmpty()) {
			//빈 이미지 업로드 시, url에 null 값 리턴
			ImageUrlResponse.builder().url(null).build();
		} else {
			try {

				// 확장자 필터링 로직, 이미지가 아닌 확장자의 file 업로드 시 예외 발생
				if (!isValidContentType(image.getContentType())) {
					throw new ImageException(INVALID_CONTENT_TYPE);
				}

				ImageUrlResponse response = ImageUrlResponse.builder()
					.url(s3UploadService.saveFile(image))
					.build();

				log.info("image save result URL is : {}", response.url());

				return response;

			} catch (IOException e) {
				throw new ImageException(FAILED_SAVE_IMAGE);
			}
		}
		return null;
	}

	// TODO :: 컨트롤러에서 어떤 방식의 요청으로 이미지를 삭제할 지 미정
	public String deleteProfileImages(String filename) {
		s3UploadService.deleteFile(filename);
		return DELETE_SUCCESS;
	}

	public boolean isValidContentType(String contentType) {
		for (ImageType type : ImageType.values()) {
			if (type.getContentType().equals(contentType)) {
				return true;
			}
		}
		return false; // 해당 contentType이 없으면 null 반환 및 호출하는 쪽에서 예외 발생
	}
}
