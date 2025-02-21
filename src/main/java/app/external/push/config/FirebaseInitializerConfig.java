package app.external.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.util.Base64;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseInitializerConfig {
	private final FirebaseProperties firebaseProperties;

	@Bean
	public void initialize() {
		if (FirebaseApp.getApps().isEmpty()) {
			try {
				String file = firebaseProperties.getBase64file();
				byte[] decodeFile = Base64.getDecoder().decode(file);
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodeFile);

				FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(byteArrayInputStream))
					.build();
				log.info("Firebase 초기화 성공 : {} ", options);
				FirebaseApp.initializeApp(options);
			} catch (Exception e) {
				log.error("Firebase 초기화 오류: {}", e.getMessage());
			}
		}
	}
}
