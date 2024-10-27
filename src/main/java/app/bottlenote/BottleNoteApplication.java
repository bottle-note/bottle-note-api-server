package app.bottlenote;

import app.external.FirebaseInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = "app")
public class BottleNoteApplication {
	public static void main(String[] args) {

		FirebaseInitializer.initialize();

		SpringApplication.run(BottleNoteApplication.class, args);
	}
}
