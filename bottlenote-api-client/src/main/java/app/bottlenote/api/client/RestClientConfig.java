package app.bottlenote.api.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;

@Configuration
public class RestClientConfig {

	@Bean
	public RestClient restClient(RestClientCustomizer... customizers) {
		RestClient.Builder builder = RestClient.builder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory())
				.defaultHeaders(headers -> {
					headers.setContentType(MediaType.APPLICATION_JSON);
					headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
				})
				.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
					throw new RestClientException("API 호출 오류: " + response.getStatusCode());
				})
				.defaultStatusHandler(
						HttpStatusCode::is4xxClientError,
						(request, response) -> {
							throw new RestClientException("클라이언트 오류: " + response.getStatusCode());
						}
				)
				.defaultStatusHandler(
						HttpStatusCode::is5xxServerError,
						(request, response) -> {
							throw new RestClientException("서버 오류: " + response.getStatusCode());
						}
				);

		// 커스터마이저 적용
		for (RestClientCustomizer customizer : customizers) {
			customizer.customize(builder);
		}

		return builder.build();
	}

	/**
	 * 기본 타임아웃 설정을 위한 RequestFactory
	 */
	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory() {
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		return factory;
	}

	/**
	 * 로깅 인터셉터 설정
	 */
	@Bean
	public RestClientCustomizer loggingCustomizer() {
		return builder -> builder.requestInitializer(request -> {
			Logger logger = LoggerFactory.getLogger(RestClientConfig.class);
			logger.debug("HTTP 요청: {} {}", request.getMethod(), request.getURI());
		});
	}
}
