package app.bottlenote.global.security.jwt;

import static app.bottlenote.global.exception.custom.code.ValidExceptionCode.JWT_TOKEN_EXCEPTION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * 인증 실패 시 호출되는 메소드입니다.
   *
   * @param request 클라이언트의 요청
   * @param response 서버의 응답
   * @param authException 인증 예외
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    log.info("JwtAuthenticationEntryPoint.commence : {}", authException.getMessage());

    Error error = resolveError(request);

    response.setStatus(error.status().value());
    response.setContentType(APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    objectMapper.writeValue(response.getWriter(), GlobalResponse.error(error).getBody());
  }

  private Error resolveError(HttpServletRequest request) {
    Exception exception = (Exception) request.getAttribute("exception");
    if (exception instanceof SignatureException) {
      return Error.of(JwtExceptionType.INVALID_SIGNATURE);
    }
    if (exception instanceof MalformedJwtException) {
      return Error.of(JwtExceptionType.MALFORMED_TOKEN);
    }
    if (exception instanceof ExpiredJwtException) {
      return Error.of(JwtExceptionType.EXPIRED_TOKEN);
    }
    if (exception instanceof UnsupportedJwtException) {
      return Error.of(JwtExceptionType.UNSUPPORTED_TOKEN);
    }
    if (exception instanceof IllegalArgumentException) {
      return Error.of(JwtExceptionType.ILLEGAL_ARGUMENT);
    }
    return Error.of(JWT_TOKEN_EXCEPTION);
  }
}
