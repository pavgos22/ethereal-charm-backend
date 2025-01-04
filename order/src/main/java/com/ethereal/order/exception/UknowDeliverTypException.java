package com.ethereal.order.exception;

public class UknowDeliverTypException extends RuntimeException {
    public UknowDeliverTypException() {
        super();
    }

    public UknowDeliverTypException(String message) {
        super(message);
    }

    public UknowDeliverTypException(String message, Throwable cause) {
        super(message, cause);
    }

    public UknowDeliverTypException(Throwable cause) {
        super(cause);
    }
}
