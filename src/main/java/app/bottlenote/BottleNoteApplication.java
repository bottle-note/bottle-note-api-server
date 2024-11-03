package app.bottlenote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EntityScan(basePackages = "app")
@SpringBootApplication(scanBasePackages = "app")
public class BottleNoteApplication {
	public static void main(String[] args) {
		SpringApplication.run(BottleNoteApplication.class, args);
	}
}
