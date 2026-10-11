package com.smnk107.DocMind.exceptions;

public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentProcessingException(Throwable cause) {
        super(cause);
    }
}