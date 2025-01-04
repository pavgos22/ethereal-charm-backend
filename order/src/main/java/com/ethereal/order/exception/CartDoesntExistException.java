package com.ethereal.order.exception;

public class CartDoesntExistException extends RuntimeException {
    public CartDoesntExistException() {
        super();
    }

    public CartDoesntExistException(String message) {
        super(message);
    }

    public CartDoesntExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public CartDoesntExistException(Throwable cause) {
        super(cause);
    }
}
