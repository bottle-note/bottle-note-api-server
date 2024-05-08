package app.bottlenote.image.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

	private final AmazonS3 amazonS3;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public String saveFile(MultipartFile multipartFile) throws IOException {

		String originalFileName = UUID.randomUUID() + "_" + multipartFile.getOriginalFilename();

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(multipartFile.getSize());
		metadata.setContentType(multipartFile.getContentType());

		log.info("upload requested file's content type is : {}", multipartFile.getContentType());

		amazonS3.putObject(bucket, originalFileName, multipartFile.getInputStream(), metadata);

		log.info("S3 upload process is success!");

		return amazonS3.getUrl(bucket, originalFileName).toString();
	}

	public void deleteFile(String fileName) {
		amazonS3.deleteObject(bucket, fileName);
		log.info("File deleted success!! filename: {}", fileName);
	}

}
