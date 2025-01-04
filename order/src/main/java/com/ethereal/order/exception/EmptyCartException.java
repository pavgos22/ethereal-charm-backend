package com.ethereal.order.exception;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException() {
        super();
    }

    public EmptyCartException(String message) {
        super(message);
    }

    public EmptyCartException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyCartException(Throwable cause) {
        super(cause);
    }
}
