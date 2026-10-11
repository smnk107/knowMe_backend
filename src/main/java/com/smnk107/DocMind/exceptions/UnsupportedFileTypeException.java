package com.smnk107.DocMind.exceptions;

public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }

    public UnsupportedFileTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedFileTypeException(Throwable cause) {
        super(cause);
    }
}
