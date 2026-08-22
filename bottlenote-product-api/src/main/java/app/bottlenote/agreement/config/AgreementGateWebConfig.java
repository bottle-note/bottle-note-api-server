package app.bottlenote.agreement.config;

import app.bottlenote.agreement.facade.AgreementFacade;
import app.bottlenote.agreement.interceptor.AgreementGateInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** product-api 전용 약관 동의 게이트 인터셉터 등록. admin-api/batch에는 영향하지 않는다. */
@Configuration
@RequiredArgsConstructor
public class AgreementGateWebConfig implements WebMvcConfigurer {

  private final AgreementFacade agreementFacade;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(new AgreementGateInterceptor(agreementFacade))
        .addPathPatterns("/api/**");
  }
}
