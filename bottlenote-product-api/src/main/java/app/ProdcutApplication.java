package app;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EntityScan(basePackages = "app")
@SpringBootApplication(scanBasePackages = "app")
@EnableScheduling
public class ProdcutApplication {
  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    SpringApplication.run(ProdcutApplication.class, args);
  }
}
