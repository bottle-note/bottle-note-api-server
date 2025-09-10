package app.bottlenote.support.block.exception;

import app.bottlenote.shared.exception.custom.AbstractCustomException;

public class BlockException extends AbstractCustomException {

  public BlockException(BlockExceptionCode blockExceptionCode) {
    super(blockExceptionCode);
  }
}
