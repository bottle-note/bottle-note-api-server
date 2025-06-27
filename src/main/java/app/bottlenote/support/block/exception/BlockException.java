package app.bottlenote.support.block.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;

public class BlockException extends AbstractCustomException {

    public BlockException(BlockExceptionCode blockExceptionCode) {
        super(blockExceptionCode);
    }
}