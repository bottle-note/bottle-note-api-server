package app.bottlenote.agreement.interceptor;

import app.bottlenote.agreement.annotation.AgreementExempt;
import app.bottlenote.agreement.exception.AgreementException;
import app.bottlenote.agreement.exception.AgreementExceptionCode;
import app.bottlenote.agreement.facade.AgreementFacade;
import app.bottlenote.global.security.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 인증된 사용자의 필수 약관 동의 충족 여부를 요청 전에 검사한다. */
@RequiredArgsConstructor
public class AgreementGateInterceptor implements HandlerInterceptor {

  private final AgreementFacade agreementFacade;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }
    if (isExempt(handlerMethod)) {
      return true;
    }

    Optional<Long> userId = SecurityContextUtil.getUserIdByContext();
    if (userId.isEmpty()) {
      return true;
    }

    if (!agreementFacade.isEligible(userId.get())) {
      throw new AgreementException(AgreementExceptionCode.AGREEMENT_REQUIRED);
    }
    return true;
  }

  private boolean isExempt(HandlerMethod handlerMethod) {
    return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), AgreementExempt.class)
        || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), AgreementExempt.class);
  }
}
