package app.external.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseInitializerConfig {

	@Value("${app.thirdParty.firebase-configuration-file}")
	private String path;


	@Bean
	public void initialize() {
		if (FirebaseApp.getApps().isEmpty()) {
			try {
				FileInputStream serviceAccount = new FileInputStream(path);
				FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(serviceAccount))
					.build();
				log.info("Firebase 초기화 성공 : {} ", options);
				FirebaseApp.initializeApp(options);
			} catch (IOException e) {
				log.error("Firebase 초기화 오류: {}", e.getMessage());
			}
		}
	}
}
