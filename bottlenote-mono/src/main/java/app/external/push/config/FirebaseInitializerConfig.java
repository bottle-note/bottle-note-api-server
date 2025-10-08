package app.external.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseInitializerConfig {
  private final FirebaseProperties firebaseProperties;

  @Bean
  public FirebaseApp initialize() {
    if (FirebaseApp.getApps().isEmpty()) {
      try {
        ClassPathResource resource =
            new ClassPathResource(firebaseProperties.getFirebaseConfigurationFile());
        InputStream serviceAccount = resource.getInputStream();
        FirebaseOptions options =
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        log.info("FirebaseApp now initialized options: {}", options);
        return FirebaseApp.initializeApp(options);
      } catch (Exception e) {
        log.error("Error initializing FirebaseApp: {}", e.getMessage());
        return null;
      }
    } else {
      log.info("FirebaseApp has already been initialized");
      return FirebaseApp.getInstance();
    }
  }
}
