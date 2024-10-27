package app.external;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
public class FirebaseInitializer {

	public static void initialize() {
		if (FirebaseApp.getApps().isEmpty()) {
			try {
				//resources/fcm/fir-push-f3076-6b8819776ae3.json
				FileInputStream serviceAccout = new FileInputStream("src/main/resources/fcm/service_account_credentials.json");

				FirebaseOptions options = new FirebaseOptions.Builder()
					.setCredentials(GoogleCredentials.fromStream(serviceAccout))
					.build();

				log.info("FirebaseApp initialized : {} ", options);

				FirebaseApp.initializeApp(options);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
