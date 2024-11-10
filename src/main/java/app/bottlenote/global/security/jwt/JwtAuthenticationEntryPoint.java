package app.bottlenote.global.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Objects;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final HandlerExceptionResolver resolver;

	public JwtAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * 인증 실패 시 호출되는 메소드입니다.
	 *
	 * @param request       클라이언트의 요청
	 * @param response      서버의 응답
	 * @param authException 인증 예외
	 */
	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) {
		log.info("JwtAuthenticationEntryPoint.commence : {}", authException.getMessage());
		// 응답 상태를 401 Unauthorized로 설정합니다.
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		// 응답 콘텐츠 타입을 JSON으로 설정합니다.
		response.setContentType(APPLICATION_JSON_VALUE);
		// 응답의 문자 인코딩을 UTF-8로 설정합니다.
		response.setCharacterEncoding("UTF-8");

		// 요청 속성에서 예외를 가져옵니다.
		Exception exception = (Exception) request.getAttribute("exception");

		if (Objects.isNull(exception))
			exception = authException;

		// 예외를 처리하기 위해 HandlerExceptionResolver를 사용합니다.
		resolver.resolveException(request, response, null, exception);
	}
}
