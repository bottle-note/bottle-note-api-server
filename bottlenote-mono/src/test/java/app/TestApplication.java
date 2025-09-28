package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @WebMvcTest가 @SpringBootConfiguration을 찾을 수 있도록 제공하는 테스트 전용 애플리케이션
// mono 모듈에는 실제 @SpringBootApplication이 없으므로 테스트를 위해 필요
@SpringBootApplication
public class TestApplication {
  public static void main(String[] args) {
    SpringApplication.run(TestApplication.class, args);
  }
}
