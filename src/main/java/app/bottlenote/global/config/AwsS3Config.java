package app.bottlenote.global.config;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsS3Config {
	@Value("${amazon.aws.accessKey}")
	private String accessKeyId;

	@Value("${amazon.aws.secretKey}")
	private String accessKeySecret;

	@Value("${amazon.aws.region}")
	private String s3RegionName;

	/**
	 * s3 의 client 를 생성한다.
	 */
	@Bean
	public AmazonS3 getAmazonS3Client() {
		final BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKeyId, accessKeySecret);

		return AmazonS3ClientBuilder
			.standard()
			.withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
			.withRegion(s3RegionName)
			.build();
	}
}
