package app.bottlenote.global.exception.custom.code;

import org.springframework.http.HttpStatus;

public interface ExceptionCode {
    String getMessage();

    HttpStatus getHttpStatus();
}
