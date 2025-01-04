package com.ethereal.order.exception;

public class UserNotLoggedInException extends RuntimeException {
    public UserNotLoggedInException() {
        super();
    }

    public UserNotLoggedInException(String message) {
        super(message);
    }

    public UserNotLoggedInException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotLoggedInException(Throwable cause) {
        super(cause);
    }
}
