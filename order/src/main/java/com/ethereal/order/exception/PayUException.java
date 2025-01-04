package com.ethereal.order.exception;

public class PayUException extends RuntimeException {
    public PayUException() {
        super();
    }

    public PayUException(String message) {
        super(message);
    }

    public PayUException(String message, Throwable cause) {
        super(message, cause);
    }

    public PayUException(Throwable cause) {
        super(cause);
    }
}
