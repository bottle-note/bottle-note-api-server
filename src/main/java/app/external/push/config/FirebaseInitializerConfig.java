package app.external.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseInitializerConfig {
	private final FirebaseProperties firebaseProperties;

	@Bean
	@ConditionalOnProperty(name = "app.third-party.firebase-configuration-file")
	public FirebaseApp initialize() {
		try {
			if (FirebaseApp.getApps().isEmpty()) {
				ClassPathResource resource = new ClassPathResource(firebaseProperties.getFirebaseConfigurationFile());
				InputStream serviceAccount = resource.getInputStream();
				FirebaseOptions options = FirebaseOptions.builder()
						.setCredentials(GoogleCredentials.fromStream(serviceAccount))
						.build();
				log.info("Firebase 초기화 성공 : {} ", options);
				return FirebaseApp.initializeApp(options);
			} else {
				return FirebaseApp.getInstance();
			}
		} catch (Exception e) {
			log.error("Firebase 초기화 오류: {}", e.getMessage());
			log.warn("Firebase를 사용하지 않고 계속 진행합니다.");
			// Firebase 설정 파일이 없어도 애플리케이션이 시작되도록 null 반환
			return null;
		}
	}
}
