package app.bottlenote.common.file.upload.fixture;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class FakeAmazonS3 extends AbstractFakeAmazonS3 {
	@Override
	public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws SdkClientException {
		URL url;
		try {
			url = new URL("http://localhost:8080");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return url;
	}

	@Override
	public URL generatePresignedUrl(String bucketName, String key, Date expiration, HttpMethod method) {
		URL url;
		try {
			url = new URL("https", bucketName + ".s3.amazonaws.com", "/" + key);
			System.out.println("Fake url 생성 : " + url);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return url;
	}

	@Override
	public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) throws SdkClientException {
		URL url;
		try {
			url = new URL("http://localhost:8080");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return url;
	}
}
