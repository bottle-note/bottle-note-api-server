package app.bottlenote.common.file.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class FileException extends AbstractCustomException {
  public FileException(FileExceptionCode code) {
    super(code);
  }
}
