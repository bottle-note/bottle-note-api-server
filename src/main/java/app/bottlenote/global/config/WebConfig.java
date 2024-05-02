package app.bottlenote.global.config;

import app.bottlenote.global.service.converter.RatingPointConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
	private final RatingPointConverter ratingPointConverter;

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(ratingPointConverter);
	}
}
