package com.ethereal.cart.exceptions;

public class NoCartInfoException extends RuntimeException {
    public NoCartInfoException() {
        super();
    }

    public NoCartInfoException(String message) {
        super(message);
    }

    public NoCartInfoException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoCartInfoException(Throwable cause) {
        super(cause);
    }
}
