package com.ethereal.order.exception;

public class BadSignatureException extends RuntimeException {
    public BadSignatureException() {
        super();
    }

    public BadSignatureException(String message) {
        super(message);
    }

    public BadSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadSignatureException(Throwable cause) {
        super(cause);
    }
}
