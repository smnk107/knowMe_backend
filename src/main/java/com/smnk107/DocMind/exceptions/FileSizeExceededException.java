package com.smnk107.DocMind.exceptions;

public class FileSizeExceededException extends RuntimeException {

    public FileSizeExceededException(String message) {
        super(message);
    }

    public FileSizeExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileSizeExceededException(Throwable cause) {
        super(cause);
    }
}
